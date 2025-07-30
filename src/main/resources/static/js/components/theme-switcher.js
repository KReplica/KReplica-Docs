function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);

    document.querySelectorAll('.theme-switcher button').forEach(btn => {
        if (btn.dataset.themeSet === theme) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
}

function handleThemeSelection(e) {
    const button = e.target.closest('button[data-theme-set]');
    if (!button) return;

    const theme = button.dataset.themeSet;
    sessionStorage.setItem('theme', theme);
    applyTheme(theme);
}

export function initThemeSwitcher() {
    const savedTheme = sessionStorage.getItem('theme') || 'light';
    applyTheme(savedTheme);

    document.querySelectorAll('.theme-switcher').forEach(switcher => {
        switcher.addEventListener('click', handleThemeSelection);
    });
}