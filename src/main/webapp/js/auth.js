/**
 * auth.js - logic for the public pages: Login, Sign Up and Forgot Password.
 * Client-side checks mirror util.ValidationUtil on the server for instant feedback;
 * the server validates again regardless.
 */
(() => {
    'use strict';

    const RULES = {
        name: /^[A-Za-z][A-Za-z .'-]{1,99}$/,
        email: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
        mobile: /^[6-9]\d{9}$/,
        username: /^[A-Za-z][A-Za-z0-9_]{3,29}$/,
        password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/
    };
    const PASSWORD_MSG = 'Password must be 8-64 characters with uppercase, lowercase, a digit and a special character.';

    const page = document.body.dataset.page;
    NB.initPasswordToggles();

    /* ------------------------------ LOGIN ------------------------------ */
    if (page === 'login') {
        const params = new URLSearchParams(location.search);
        if (params.has('logout')) NB.toast('You have been logged out successfully.', 'info');
        if (params.has('expired')) NB.toast('Please log in to continue.', 'info');
        if (params.has('reset')) NB.toast('Password reset successful. Please log in.', 'success');

        const form = document.getElementById('loginForm');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            NB.clearErrors(form);
            const data = NB.formData(form);
            const errors = {};
            if (!data.identifier.trim()) errors.identifier = 'Username or email is required.';
            if (!data.password) errors.password = 'Password is required.';
            if (Object.keys(errors).length) return NB.showErrors(form, errors);

            const btn = form.querySelector('button[type=submit]');
            NB.setBusy(btn, true, 'Signing in...');
            const res = await NB.request('api/login', { method: 'POST', data });
            NB.setBusy(btn, false);

            if (res.ok && res.body.success) {
                // The dashboard shows the welcome popup once, using this flag.
                sessionStorage.setItem('nb_welcome', res.body.data.firstName);
                window.location.replace('dashboard');
            } else {
                NB.showErrors(form, res.body.errors);
                NB.toast(res.body.message || 'Login failed.', 'error');
                form.password.value = '';
            }
        });
    }

    /* ------------------------------ SIGN UP ----------------------------- */
    if (page === 'signup') {
        const form = document.getElementById('signupForm');
        const pwd = document.getElementById('password');
        const bar = document.getElementById('strengthBar');

        // Password strength meter (0-5 points)
        pwd.addEventListener('input', () => {
            const v = pwd.value;
            let score = 0;
            if (v.length >= 8) score++;
            if (/[a-z]/.test(v)) score++;
            if (/[A-Z]/.test(v)) score++;
            if (/\d/.test(v)) score++;
            if (/[^A-Za-z0-9]/.test(v)) score++;
            bar.style.width = (score * 20) + '%';
            bar.className = score <= 2 ? 'weak' : (score <= 4 ? 'medium' : 'strong');
        });

        // Allow digits only in the mobile field
        form.mobile.addEventListener('input', () => {
            form.mobile.value = form.mobile.value.replace(/\D/g, '').slice(0, 10);
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            NB.clearErrors(form);
            const d = NB.formData(form);
            const errors = {};
            if (!RULES.name.test(d.fullName.trim())) errors.fullName = 'Enter your full name (letters only, 2-100 characters).';
            if (!RULES.email.test(d.email.trim())) errors.email = 'Enter a valid email address.';
            if (!RULES.mobile.test(d.mobile.trim())) errors.mobile = 'Enter a valid 10-digit mobile number starting with 6-9.';
            if (!RULES.username.test(d.username.trim())) errors.username = 'Username: 4-30 characters, start with a letter; letters, digits, _ only.';
            if (!RULES.password.test(d.password)) errors.password = PASSWORD_MSG;
            if (!d.confirmPassword) errors.confirmPassword = 'Please confirm your password.';
            else if (d.password !== d.confirmPassword) errors.confirmPassword = 'Passwords do not match.';
            if (d.address.trim().length < 5) errors.address = 'Address must be at least 5 characters.';
            if (Object.keys(errors).length) {
                NB.toast('Please correct the highlighted fields.', 'error');
                return NB.showErrors(form, errors);
            }

            const btn = form.querySelector('button[type=submit]');
            NB.setBusy(btn, true, 'Creating account...');
            const res = await NB.request('api/signup', { method: 'POST', data: d });
            NB.setBusy(btn, false);

            if (res.ok && res.body.success) {
                const acc = res.body.data;
                form.reset();
                bar.style.width = '0';
                NB.modal({
                    title: 'Welcome to Net Banking!',
                    message: 'Your account has been successfully created.',
                    type: 'success',
                    highlight: { label: 'Your Account Number', value: acc.accountNumber },
                    details: [
                        ['Account Holder', acc.fullName],
                        ['Username', acc.username],
                        ['Account Type', acc.accountType],
                        ['Opening Balance', NB.formatINR(acc.balance)]
                    ],
                    buttonText: 'Continue to Login',
                    onClose: () => window.location.href = 'login'
                });
            } else {
                NB.showErrors(form, res.body.errors);
                NB.toast(res.body.message || 'Registration failed.', 'error');
            }
        });
    }

    /* -------------------------- FORGOT PASSWORD -------------------------- */
    if (page === 'forgot') {
        const form = document.getElementById('forgotForm');
        form.mobile.addEventListener('input', () => {
            form.mobile.value = form.mobile.value.replace(/\D/g, '').slice(0, 10);
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            NB.clearErrors(form);
            const d = NB.formData(form);
            const errors = {};
            if (!d.username.trim()) errors.username = 'Username is required.';
            if (!RULES.email.test(d.email.trim())) errors.email = 'Enter your registered email.';
            if (!RULES.mobile.test(d.mobile.trim())) errors.mobile = 'Enter your registered 10-digit mobile number.';
            if (!RULES.password.test(d.newPassword)) errors.newPassword = PASSWORD_MSG;
            if (d.newPassword !== d.confirmPassword) errors.confirmPassword = 'Passwords do not match.';
            if (Object.keys(errors).length) return NB.showErrors(form, errors);

            const btn = form.querySelector('button[type=submit]');
            NB.setBusy(btn, true, 'Resetting...');
            const res = await NB.request('api/forgot-password', { method: 'POST', data: d });
            NB.setBusy(btn, false);

            if (res.ok && res.body.success) {
                NB.modal({
                    title: 'Password Reset',
                    message: res.body.message,
                    type: 'success',
                    buttonText: 'Go to Login',
                    onClose: () => window.location.href = 'login?reset=1'
                });
            } else {
                NB.showErrors(form, res.body.errors);
                NB.toast(res.body.message || 'Password reset failed.', 'error');
            }
        });
    }
})();
