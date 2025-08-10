export function navMenu() {
    return {
        isNavOpen: false,
        toggle() {
            this.isNavOpen = !this.isNavOpen;
        },
        close() {
            this.isNavOpen = false;
        }
    };
}