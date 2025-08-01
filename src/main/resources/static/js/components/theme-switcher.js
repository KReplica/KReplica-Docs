const THEME_TO_MONACO = {
    light: 'vs-dark',
    blue: 'vs',
    dark: 'vs-dark'
};

export function themeSwitcher() {
    return {
        themes: ['light', 'blue', 'dark'],
        theme: 'light',
        nextTheme: 'blue',
        get monacoTheme() {
            return THEME_TO_MONACO[this.theme] || 'vs-dark';
        },
        init() {
            this.theme = localStorage.getItem('theme') || 'light';
            document.documentElement.setAttribute('data-theme', this.theme);
            window.dispatchEvent(new CustomEvent('theme-changed', {
                detail: {
                    theme: this.theme,
                    monacoTheme: this.monacoTheme
                }
            }));
            this.updateNextTheme();
            this.$watch('theme', (newTheme) => {
                localStorage.setItem('theme', newTheme);
                document.documentElement.setAttribute('data-theme', newTheme);
                window.dispatchEvent(new CustomEvent('theme-changed', {
                    detail: {
                        theme: newTheme,
                        monacoTheme: this.monacoTheme
                    }
                }));
                this.updateNextTheme();
            });
        },
        applyTheme(theme) {
            this.theme = theme;
        },
        cycleTheme() {
            const i = this.themes.indexOf(this.theme);
            this.applyTheme(this.themes[(i + 1) % this.themes.length]);
        },
        updateNextTheme() {
            const i = this.themes.indexOf(this.theme);
            this.nextTheme = this.themes[(i + 1) % this.themes.length];
        }
    };
}