export const THEMES = ["light", "blue", "dark"];

const THEME_TO_MONACO = {
    light: "vs-dark",
    blue: "vs",
    dark: "vs-dark"
};

export const getMonacoTheme = theme => {
    const monaco = THEME_TO_MONACO[theme];
    if (!monaco) throw new Error(`Unknown theme: ${theme}`);
    return monaco;
};

export const initTheme = () => {
    const stored = localStorage.getItem("theme") || "light";
    document.documentElement.setAttribute("data-theme", stored);
    window.dispatchEvent(
        new CustomEvent("theme-changed", {
            detail: {theme: stored, monacoTheme: getMonacoTheme(stored)}
        })
    );
    return stored;
};

export const setTheme = theme => {
    if (!THEME_TO_MONACO[theme]) throw new Error(`Unknown theme: ${theme}`);
    localStorage.setItem("theme", theme);
    document.documentElement.setAttribute("data-theme", theme);
    window.dispatchEvent(
        new CustomEvent("theme-changed", {
            detail: {theme, monacoTheme: getMonacoTheme(theme)}
        })
    );
};