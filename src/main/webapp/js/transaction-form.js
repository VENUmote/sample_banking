/**
 * transaction-form.js - shared by deposit.html and withdraw.html.
 * The form's data-type attribute ("deposit" | "withdraw") selects the API endpoint.
 */
(async () => {
    'use strict';

    const form = document.getElementById('txnForm');
    const kind = form.dataset.type;                 // deposit | withdraw
    const isDeposit = kind === 'deposit';
    const balanceEl = document.getElementById('currentBalance');
    const AMOUNT_RE = /^\d{1,13}(\.\d{1,2})?$/;
    const MAX = 1000000;

    const me = await NB.initProtectedPage(kind);
    if (!me) return;

    let currentBalance = Number(me.balance);
    form.accountNumber.value = me.accountNumber;
    balanceEl.textContent = NB.formatINR(currentBalance);

    // Quick-amount buttons
    document.querySelectorAll('.quick-amounts button').forEach(b => {
        b.addEventListener('click', () => {
            form.amount.value = b.dataset.amount;
            form.amount.focus();
        });
    });

    // Allow only digits and one decimal point while typing
    form.amount.addEventListener('input', () => {
        let v = form.amount.value.replace(/[^\d.]/g, '');
        const parts = v.split('.');
        if (parts.length > 2) v = parts[0] + '.' + parts.slice(1).join('');
        form.amount.value = v;
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        NB.clearErrors(form);
        const data = NB.formData(form);
        data.amount = data.amount.trim();
        const amount = Number(data.amount);

        // Client-side validation (server re-validates)
        const errors = {};
        if (!AMOUNT_RE.test(data.amount) || !(amount > 0)) {
            errors.amount = 'Enter a positive amount with up to 2 decimal places.';
        } else if (amount > MAX) {
            errors.amount = 'Maximum per transaction is ₹10,00,000.00.';
        } else if (!isDeposit && amount > currentBalance) {
            errors.amount = 'Amount exceeds your available balance of ' + NB.formatINR(currentBalance) + '.';
        }
        if (Object.keys(errors).length) {
            if (!isDeposit && errors.amount && errors.amount.startsWith('Amount exceeds')) {
                showInsufficient(currentBalance, amount);
            }
            return NB.showErrors(form, errors);
        }

        const btn = form.querySelector('button[type=submit]');
        NB.setBusy(btn, true, 'Processing...');
        const res = await NB.request('api/' + kind, { method: 'POST', data });
        NB.setBusy(btn, false);

        if (res.ok && res.body.success) {
            const r = res.body.data;
            currentBalance = Number(r.newBalance);
            balanceEl.textContent = NB.formatINR(currentBalance);
            form.amount.value = '';
            form.description.value = '';
            NB.toast(res.body.message, 'success');
            NB.modal({
                title: isDeposit ? 'Deposit Successful' : 'Withdrawal Successful',
                message: res.body.message,
                type: 'success',
                highlight: { label: isDeposit ? 'New Balance' : 'Remaining Balance', value: NB.formatINR(r.newBalance) },
                details: [
                    ['Transaction ID', '#' + r.transactionId],
                    ['Account Number', r.accountNumber],
                    ['Previous Balance', NB.formatINR(r.previousBalance)],
                    [isDeposit ? 'Deposit Amount' : 'Withdrawal Amount', NB.formatINR(r.amount)],
                    [isDeposit ? 'New Balance' : 'Remaining Balance', NB.formatINR(r.newBalance)],
                    ['Description', r.description]
                ],
                buttonText: 'Done'
            });
        } else if (res.status === 422) {
            const d = res.body.data || {};
            currentBalance = Number(d.availableBalance);
            balanceEl.textContent = NB.formatINR(currentBalance);
            NB.showErrors(form, res.body.errors);
            showInsufficient(d.availableBalance, d.requestedAmount);
        } else {
            NB.showErrors(form, res.body.errors);
            NB.toast(res.body.message || 'Transaction failed.', 'error');
        }
    });

    function showInsufficient(available, requested) {
        NB.modal({
            title: 'Insufficient Balance',
            message: 'You cannot withdraw more than your available balance.',
            type: 'error',
            details: [
                ['Available Balance', NB.formatINR(available)],
                ['Requested Amount', NB.formatINR(requested)]
            ],
            buttonText: 'OK'
        });
    }
})();
