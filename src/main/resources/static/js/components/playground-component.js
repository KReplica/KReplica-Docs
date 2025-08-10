export function playgroundComponent() {
    return {
        activeTab: 'editor',
        isOutputReady: false,
        tabSessionId: window.generateUniqueId(),
        isSubmitting: false,
        onOutputReady() {
            this.isOutputReady = true;
            if (window.innerWidth < 992) {
                this.activeTab = 'output';
            }
        },
        onClearOutput() {
            this.isOutputReady = false;
            this.activeTab = 'editor';
        }
    };
}