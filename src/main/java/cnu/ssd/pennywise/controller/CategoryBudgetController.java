package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.CategoryBudgetPanel;
import cnu.ssd.pennywise.gui.CategoryFormDialog;
import cnu.ssd.pennywise.model.BudgetLimit;
import cnu.ssd.pennywise.model.Category;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.model.TransactionType;
import cnu.ssd.pennywise.exception.BudgetException;
import cnu.ssd.pennywise.service.BudgetService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.springframework.stereotype.Component;

/**
 * Drives {@link CategoryBudgetPanel} and {@link CategoryFormDialog}: loads the
 * current household's categories and budget overview, and handles category
 * create/edit/delete plus setting the household-wide limit.
 * Every call into {@link BudgetService} runs on a {@link SwingWorker}
 * so a slow database round trip never freezes the EDT.
 */
@Component
public class CategoryBudgetController {

    private final BudgetService budgetService;
    private final HouseholdContext householdContext;
    private final AppFrame appFrame;
    private final CategoryBudgetPanel panel = new CategoryBudgetPanel();

    public CategoryBudgetController(BudgetService budgetService, HouseholdContext householdContext, AppFrame appFrame) {
        this.budgetService = budgetService;
        this.householdContext = householdContext;
        this.appFrame = appFrame;
        wireListeners();
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Reloads the category table and the budget-overview bars from the current household. */
    public void refresh() {
        Long householdId = householdContext.getCurrentHouseholdId();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                List<Category> categories = budgetService.getCategories(householdId);
                List<BudgetLimit> overview = budgetService.getBudgetOverview(householdId);
                return new Object[]{categories, overview};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    panel.setCategories((List<Category>) result[0]);
                    panel.setBudgetOverview((List<BudgetLimit>) result[1]);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(appFrame.getFrame(), "Could not load categories & budgets. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void wireListeners() {
        panel.getAddButton().addActionListener(e -> openCreateDialog());
        panel.getEditButton().addActionListener(e -> openEditDialog());
        panel.getDeleteButton().addActionListener(e -> handleDelete());
        panel.getSaveGlobalLimitButton().addActionListener(e -> handleSaveGlobalLimit());
    }

    private void openCreateDialog() {
        CategoryFormDialog dialog = new CategoryFormDialog(appFrame.getFrame(), false);
        dialog.getSaveButton().addActionListener(e -> handleCreate(dialog));
        dialog.setVisible(true);
    }

    private void openEditDialog() {
        Category category = panel.getSelectedCategory();
        if (category == null) {
            showError(panel, "Select a category to edit first.");
            return;
        }
        CategoryFormDialog dialog = new CategoryFormDialog(appFrame.getFrame(), true);
        dialog.populateFrom(category);
        dialog.getSaveButton().addActionListener(e -> handleUpdate(dialog, category.getId()));
        dialog.setVisible(true);
    }

    private void handleCreate(CategoryFormDialog dialog) {
        Long householdId = householdContext.getCurrentHouseholdId();
        String name = dialog.getNameField().getText();
        TransactionType type = typeOf(dialog.getTypeComboBox());
        BigDecimal limit = limitOf(dialog);

        dialog.getSaveButton().setEnabled(false);
        new SwingWorker<Category, Void>() {
            @Override
            protected Category doInBackground() {
                return budgetService.createCategory(householdId, name, type, limit);
            }

            @Override
            protected void done() {
                dialog.getSaveButton().setEnabled(true);
                try {
                    get();
                    dialog.dispose();
                    refresh();
                } catch (ExecutionException ex) {
                    handleServiceError(ex, dialog);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleUpdate(CategoryFormDialog dialog, Long categoryId) {
        String name = dialog.getNameField().getText();
        TransactionType type = typeOf(dialog.getTypeComboBox());
        BigDecimal limit = limitOf(dialog);

        dialog.getSaveButton().setEnabled(false);
        new SwingWorker<Category, Void>() {
            @Override
            protected Category doInBackground() {
                return budgetService.updateCategory(categoryId, name, type, limit);
            }

            @Override
            protected void done() {
                dialog.getSaveButton().setEnabled(true);
                try {
                    get();
                    dialog.dispose();
                    refresh();
                } catch (ExecutionException ex) {
                    handleServiceError(ex, dialog);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleDelete() {
        Category category = panel.getSelectedCategory();
        if (category == null) {
            showError(panel, "Select a category to delete first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(panel, "Delete category \"" + category.getName() + "\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        panel.getDeleteButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                budgetService.deleteCategory(category.getId());
                return null;
            }

            @Override
            protected void done() {
                panel.getDeleteButton().setEnabled(true);
                try {
                    get();
                    refresh();
                } catch (ExecutionException ex) {
                    handleServiceError(ex, panel);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleSaveGlobalLimit() {
        Long householdId = householdContext.getCurrentHouseholdId();
        BigDecimal limit = parseAmount(panel.getGlobalLimitField().getText());
        if (panel.getGlobalLimitField().getText().isBlank()) {
            limit = null;
        } else if (limit == null) {
            showError(panel, "Enter a valid, non-negative limit (or leave it blank to remove the limit).");
            return;
        }
        BigDecimal finalLimit = limit;

        panel.getSaveGlobalLimitButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                budgetService.setGlobalBudgetLimit(householdId, finalLimit);
                return null;
            }

            @Override
            protected void done() {
                panel.getSaveGlobalLimitButton().setEnabled(true);
                try {
                    get();
                    panel.getGlobalLimitField().setText("");
                    refresh();
                } catch (ExecutionException ex) {
                    handleServiceError(ex, panel);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleServiceError(ExecutionException ex, java.awt.Component parent) {
        if (ex.getCause() instanceof BudgetException) {
            showError(parent, ex.getCause().getMessage());
        } else {
            ex.getCause().printStackTrace();
            showError(parent, "Something went wrong. Please try again.");
        }
    }

    private TransactionType typeOf(javax.swing.JComboBox<String> combo) {
        return "Income".equals(combo.getSelectedItem()) ? TransactionType.INCOME : TransactionType.EXPENSE;
    }

    private BigDecimal limitOf(CategoryFormDialog dialog) {
        return dialog.getHasLimitCheckBox().isSelected() ? parseAmount(dialog.getLimitField().getText()) : null;
    }

    private BigDecimal parseAmount(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.signum() >= 0 ? value : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Invalid data", JOptionPane.ERROR_MESSAGE);
    }
}
