export function copyButton() {
    return {
        copied: false,
        copy(element) {
            if (!element) return;
            navigator.clipboard.writeText(element.textContent).then(() => {
                this.copied = true;
                setTimeout(() => {
                    this.copied = false;
                }, 2000);
            }).catch(err => {
                console.error('Failed to copy text: ', err);
            });
        },
        get buttonText() {
            return this.copied ? 'Copied!' : 'Copy';
        }
    };
}