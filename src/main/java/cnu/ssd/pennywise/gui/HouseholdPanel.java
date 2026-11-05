package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.HouseholdController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.Member;
import cnu.ssd.pennywise.model.MemberRole;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The household members / invitations view (View / Boundary): a table of
 * members with their role, a "Remove member" action, and the current
 * invitation link with a "Generate / Copy link" button.
 * <p>
 * Contains no business logic: it only renders whatever {@link #setMembers}
 * is given.
 */
public class HouseholdPanel extends JPanel {

    private static final String[] COLUMNS = {"Member", "Role"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final AeroButton removeMemberButton = new AeroButton("Remove member", UiTheme.RED);

    private final javax.swing.JTextField usernameField = new javax.swing.JTextField(16);
    private final AeroButton addMemberButton = new AeroButton("Add member", UiTheme.GREEN);

    private List<Member> members = List.of();

    public HouseholdPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JLabel title = new JLabel("Household");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 14, 0));
        body.setOpaque(false);
        body.add(buildMembersSection());
        body.add(buildInviteSection());
        add(body, BorderLayout.CENTER);
    }

    private AeroPanel buildMembersSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Members"), BorderLayout.NORTH);
        section.add(AeroTableStyler.style(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(removeMemberButton);
        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private AeroPanel buildInviteSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Add a member"), BorderLayout.NORTH);

        JLabel hint = new JLabel("<html>Enter the username of a registered user. They are added right away and "
                + "will see this household when they log in.</html>");
        hint.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        hint.setForeground(UiTheme.MUTED);
        section.add(hint, BorderLayout.CENTER);

        usernameField.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        usernameField.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(new java.awt.Color(0x9CCBEA), 1, true),
                new javax.swing.border.EmptyBorder(5, 8, 5, 8)));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(new JLabel("Username:"));
        actions.add(usernameField);
        actions.add(addMemberButton);
        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    /** Rebuilds the member table. Keeps the given list so selection can be mapped back to a {@link Member}. */
    public void setMembers(List<Member> members) {
        this.members = members;
        tableModel.setRowCount(0);
        for (Member member : members) {
            tableModel.addRow(new Object[]{
                    member.getUser().getUsername(),
                    member.getRole() == MemberRole.HOST ? "Host" : "Member"
            });
        }
    }

    /** The currently selected row's member, or {@code null} if nothing is selected. */
    public Member getSelectedMember() {
        int row = table.getSelectedRow();
        return (row < 0 || row >= members.size()) ? null : members.get(row);
    }

    public AeroButton getRemoveMemberButton() {
        return removeMemberButton;
    }

    public javax.swing.JTextField getUsernameField() {
        return usernameField;
    }

    public AeroButton getAddMemberButton() {
        return addMemberButton;
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        l.setForeground(UiTheme.DEEP);
        return l;
    }
}
