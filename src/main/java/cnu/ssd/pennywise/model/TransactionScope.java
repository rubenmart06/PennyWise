package cnu.ssd.pennywise.model;

/** Whether a record affects only its payer, or is split across household members. */
public enum TransactionScope {
    INDIVIDUAL,
    SHARED
}
