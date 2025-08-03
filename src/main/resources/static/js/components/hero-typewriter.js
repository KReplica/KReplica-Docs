export function heroTypewriter() {
    return {
        isPaneVisible: false,
        isTyping: false,
        isComplete: false,
        init() {
            const observer = new IntersectionObserver((entries) => {
                if (entries[0].isIntersecting) {
                    this.startAnimation();
                    observer.disconnect();
                }
            }, {threshold: 0.5});

            observer.observe(this.$el);
        },
        startAnimation() {
            if (this.isComplete) return;

            this.isPaneVisible = true;

            setTimeout(() => {
                const sourceText = this.$refs.sourceCodeTemplate.textContent || '';
                const outputEl = this.$refs.outputCode;
                let charIndex = 0;

                this.isTyping = true;
                outputEl.textContent = '';

                const typeChar = () => {
                    if (charIndex < sourceText.length) {
                        outputEl.textContent += sourceText[charIndex];
                        charIndex++;
                        setTimeout(typeChar, 15);
                    } else {
                        this.isTyping = false;
                        this.isComplete = true;
                        this.$nextTick(() => {
                            Prism.highlightElement(outputEl);
                        });
                    }
                };
                typeChar();
            }, 600);
        }
    };
}