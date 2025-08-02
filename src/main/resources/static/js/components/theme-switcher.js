import {getMonacoTheme, initTheme, setTheme, THEMES} from "../utils/theme.js";

export function themeSwitcher() {
    return {
        themes: THEMES,
        theme: "light",
        get monacoTheme() {
            return getMonacoTheme(this.theme);
        },
        get nextTheme() {
            const i = this.themes.indexOf(this.theme);
            return this.themes[(i + 1) % this.themes.length];
        },
        init() {
            this.theme = initTheme();
            this.$watch("theme", newTheme => {
                setTheme(newTheme);
            });
        },
        applyTheme(theme) {
            this.theme = theme;
        },
        cycleTheme() {
            this.applyTheme(this.nextTheme);
        }
    };
}