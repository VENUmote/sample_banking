/**
 * dashboard.js - loads /api/dashboard and fills the balance card, summary cards and the
 * recent-transactions table. Shows the "Welcome back" popup once after login.
 */
(async () => {
    'use strict';

    const me = await NB.initProtectedPage('dashboard');
    if (!me) return;

    // One-time welcome popup (flag set by auth.js after a successful login)
    const firstName = sessionStorage.getItem('nb_welcome');
    if (firstName) {
        sessionStorage.removeItem('nb_welcome');
        NB.modal({
            title: 'Welcome back, ' + firstName + '!',
            message: 'You have successfully logged into Net Banking.',
            type: 'success',
            buttonText: 'Go to Dashboard'
        });
    }

    const res = await NB.request('api/dashboard');
    if (!res.ok) {
        NB.toast(res.body.message || 'Unable to load dashboard.', 'error');
        return;
    }
    const d = res.body.data;
    const set = (id, text) => { document.getElementById(id).textContent = text; };

    set('customerName', d.customerName);
    set('customerNameCard', d.customerName);
    set('accountNumber', d.accountNumber);
    set('accountType', d.accountType);
    set('currentBalance', NB.formatINR(d.currentBalance));
    set('totalCredit', NB.formatINR(d.totalCredit));
    set('totalDebit', NB.formatINR(d.totalDebit));
    set('totalTransactions', d.totalTransactions);

    const verify = document.getElementById('balanceVerify');
    verify.textContent = d.balanceVerified
        ? '✓ Verified: Total Credit − Total Debit = Current Balance'
        : '⚠ Balance reconciliation mismatch - please contact the bank';
    verify.classList.add(d.balanceVerified ? 'ok' : 'bad');

    // Recent transactions table
    const tbody = document.getElementById('recentBody');
    tbody.replaceChildren();
    if (!d.recentTransactions.length) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 6;
        td.className = 'empty';
        td.textContent = 'No transactions yet. Make your first deposit to get started!';
        tr.appendChild(td);
        tbody.appendChild(tr);
        return;
    }
    d.recentTransactions.forEach(t => {
        const tr = document.createElement('tr');
        if (t.status === 'FAILED') tr.className = 'row-failed';
        const cells = [
            '#' + t.transactionId,
            NB.formatDateTime(t.transactionDate),
            null,
            t.description || '-',
            (t.transactionType === 'CREDIT' ? '+ ' : '- ') + NB.formatINR(t.amount),
            NB.formatINR(t.balanceAfter)
        ];
        cells.forEach((text, i) => {
            const td = document.createElement('td');
            if (i === 2) {
                const badge = document.createElement('span');
                badge.className = 'badge badge-' + t.transactionType.toLowerCase();
                badge.textContent = t.status === 'FAILED' ? t.transactionType + ' (FAILED)' : t.transactionType;
                td.appendChild(badge);
            } else {
                td.textContent = text;
            }
            if (i >= 4) td.className = 'num';
            if (i === 4) td.classList.add(t.transactionType === 'CREDIT' ? 'amount-credit' : 'amount-debit');
            td.dataset.label = ['Txn ID', 'Date', 'Type', 'Description', 'Amount', 'Balance'][i];
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
})();
