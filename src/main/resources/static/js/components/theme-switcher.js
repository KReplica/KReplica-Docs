export function themeSwitcher() {
    return {
        themes: ['light', 'blue', 'dark'],
        theme: 'light',
        nextTheme: 'blue',

        get monacoTheme() {
            return this.theme === 'dark' ? 'vs-dark' : 'vs';
        },

        init() {
            this.theme = localStorage.getItem('theme') || 'light';
            this.applyTheme(this.theme);

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
            const currentIndex = this.themes.indexOf(this.theme);
            const nextIndex = (currentIndex + 1) % this.themes.length;
            this.applyTheme(this.themes[nextIndex]);
        },

        updateNextTheme() {
            const currentIndex = this.themes.indexOf(this.theme);
            const nextIndex = (currentIndex + 1) % this.themes.length;
            this.nextTheme = this.themes[nextIndex];
        }
    };
}