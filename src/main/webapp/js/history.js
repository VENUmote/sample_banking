/**
 * history.js - Transaction History page: search, CREDIT/DEBIT filter, date range and sort.
 * Every change calls GET /api/transactions with the filters as query parameters, so the
 * data always comes fresh from MySQL.
 */
(async () => {
    'use strict';

    const me = await NB.initProtectedPage('transactions');
    if (!me) return;

    const form = document.getElementById('filterForm');
    const tbody = document.getElementById('historyBody');
    const info = document.getElementById('resultInfo');
    let debounceTimer = null;

    async function load() {
        const f = NB.formData(form);
        if (f.from && f.to && f.from > f.to) {
            NB.toast("'From' date must be on or before 'To' date.", 'error');
            return;
        }
        const params = new URLSearchParams();
        Object.entries(f).forEach(([k, v]) => { if (v) params.set(k, v.trim()); });

        info.textContent = 'Loading...';
        const res = await NB.request('api/transactions?' + params.toString());
        if (!res.ok) {
            info.textContent = '';
            NB.toast(res.body.message || 'Unable to load transactions.', 'error');
            return;
        }
        render(res.body.data);
    }

    function render(list) {
        tbody.replaceChildren();
        let credit = 0;
        let debit = 0;

        if (!list.length) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 8;
            td.className = 'empty';
            td.textContent = 'No transactions match your filters.';
            tr.appendChild(td);
            tbody.appendChild(tr);
        }

        list.forEach(t => {
            const ok = t.status === 'SUCCESS';
            if (ok && t.creditAmount != null) credit += Number(t.creditAmount);
            if (ok && t.debitAmount != null) debit += Number(t.debitAmount);

            const tr = document.createElement('tr');
            if (!ok) tr.className = 'row-failed';
            const labels = ['Txn ID', 'Date & Time', 'Type', 'Description', 'Credit', 'Debit', 'Balance After', 'Status'];
            const values = [
                '#' + t.transactionId,
                NB.formatDateTime(t.transactionDate),
                null,
                t.description || '-',
                t.creditAmount != null ? NB.formatINR(t.creditAmount) : '-',
                t.debitAmount != null ? NB.formatINR(t.debitAmount) : '-',
                NB.formatINR(t.balanceAfter),
                null
            ];
            values.forEach((v, i) => {
                const td = document.createElement('td');
                td.dataset.label = labels[i];
                if (i === 2) {
                    const b = document.createElement('span');
                    b.className = 'badge badge-' + t.transactionType.toLowerCase();
                    b.textContent = t.transactionType;
                    td.appendChild(b);
                } else if (i === 7) {
                    const b = document.createElement('span');
                    b.className = 'status ' + (ok ? 'status-ok' : 'status-failed');
                    b.textContent = t.status;
                    td.appendChild(b);
                } else {
                    td.textContent = v;
                }
                if (i >= 4 && i <= 6) td.className = 'num';
                if (i === 4 && t.creditAmount != null) td.classList.add('amount-credit');
                if (i === 5 && t.debitAmount != null) td.classList.add('amount-debit');
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });

        info.textContent = list.length + ' transaction(s)' + (list.length >= 500 ? ' (showing latest 500)' : '');
        document.getElementById('sumCredit').textContent = NB.formatINR(credit);
        document.getElementById('sumDebit').textContent = NB.formatINR(debit);
    }

    form.addEventListener('submit', (e) => { e.preventDefault(); load(); });
    form.type.addEventListener('change', load);
    form.sort.addEventListener('change', load);
    form.from.addEventListener('change', load);
    form.to.addEventListener('change', load);
    form.q.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(load, 350);           // search as you type
    });
    form.addEventListener('reset', () => setTimeout(load, 0));

    load();
})();
