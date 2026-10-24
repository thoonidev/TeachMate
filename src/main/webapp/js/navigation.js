/* Native details remains usable if JavaScript is unavailable. */
document.addEventListener('click', (event) => {
    document.querySelectorAll('.tm-account[open]').forEach((account) => {
        if (!account.contains(event.target)) account.open = false;
    });
});

document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') return;
    document.querySelectorAll('.tm-account[open]').forEach((account) => {
        const hadFocus = account.contains(document.activeElement);
        account.open = false;
        if (hadFocus) account.querySelector('summary').focus();
    });
});
