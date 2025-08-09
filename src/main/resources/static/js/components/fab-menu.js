export function fabMenu() {
    return {
        isOpen: false,
        openSection: null,
        init() {
            this.$watch('isOpen', value => {
                if (!value) {
                    this.openSection = null;
                }
            });
        },
        toggleSection(sectionId) {
            this.openSection = this.openSection === sectionId ? null : sectionId;
        },
        close() {
            this.isOpen = false;
        }
    };
}