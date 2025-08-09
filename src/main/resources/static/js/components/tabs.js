export function tabs(defaultTab) {
    return {
        activeTab: defaultTab,
        isActive(tabId) {
            return this.activeTab === tabId;
        },
        activate(tabId) {
            this.activeTab = tabId;
        }
    };
}