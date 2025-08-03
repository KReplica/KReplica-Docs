export function heroTypewriter() {
    return {
        isPaneVisible: false,
        isTyping: false,
        isComplete: false,
        _started: false,
        _fallbackTimer: null,
        _debugId: Math.random().toString(36).slice(2),
        init() {
            try {
                console.group("[HeroTypewriter:init]", this._debugId);
                console.log("Element", this.$el);
                console.log("Refs at init", this.$refs);
                const startIfNeeded = () => {
                    if (!this._started) {
                        console.log("Fallback start triggered");
                        this.startAnimation();
                    } else {
                        console.log("Fallback checked but already started");
                    }
                };
                this._fallbackTimer = setTimeout(startIfNeeded, 4000);
                if (typeof IntersectionObserver === "function") {
                    console.log("IntersectionObserver available");
                    const observer = new IntersectionObserver((entries) => {
                        const entry = entries && entries[0];
                        const visible = !!(entry && entry.isIntersecting);
                        console.log("Observer callback", {visible, ratio: entry ? entry.intersectionRatio : null});
                        if (visible) {
                            this.startAnimation();
                            observer.disconnect();
                        }
                    }, {threshold: 0.3});
                    observer.observe(this.$el);
                    console.log("Observer attached");
                } else {
                    console.log("IntersectionObserver not available, starting immediately");
                    this.startAnimation();
                }
                console.groupEnd();
            } catch (e) {
                console.error("[HeroTypewriter:init] error", e);
                this.startAnimation();
            }
        },
        startAnimation() {
            if (this._started) {
                console.log("[HeroTypewriter:startAnimation] already started");
                return;
            }
            console.group("[HeroTypewriter:startAnimation]", this._debugId);
            this._started = true;
            this.isPaneVisible = true;
            if (this._fallbackTimer) {
                clearTimeout(this._fallbackTimer);
                this._fallbackTimer = null;
            }
            setTimeout(() => {
                try {
                    const tpl = this.$refs.sourceCodeTemplate;
                    const outputEl = this.$refs.outputCode;
                    console.log("Refs before typing", {tplExists: !!tpl, outputExists: !!outputEl});
                    if (!tpl || !outputEl) {
                        console.warn("Missing refs, aborting typing");
                        console.groupEnd();
                        return;
                    }
                    let sourceText = "";
                    if (tpl.content && typeof tpl.content.querySelector === "function") {
                        const codeNode = tpl.content.querySelector("code") || tpl.content.firstChild;
                        sourceText = codeNode && codeNode.textContent ? codeNode.textContent : "";
                        console.log("Read from template.content", {hasContent: !!tpl.content, hasCodeNode: !!codeNode});
                    } else {
                        sourceText = tpl.textContent || "";
                        console.log("Read from template.textContent fallback");
                    }
                    console.log("Source length", sourceText.length);
                    let charIndex = 0;
                    this.isTyping = true;
                    outputEl.textContent = "";
                    const tick = () => {
                        if (charIndex < sourceText.length) {
                            outputEl.textContent += sourceText[charIndex];
                            if (charIndex % 50 === 0) {
                                console.log("Typed chars", charIndex);
                            }
                            charIndex++;
                            setTimeout(tick, 15);
                        } else {
                            console.log("Typing complete");
                            this.isTyping = false;
                            this.isComplete = true;
                            this.$nextTick(() => {
                                try {
                                    if (window.Prism && typeof Prism.highlightElement === "function") {
                                        console.log("Prism found, highlighting");
                                        Prism.highlightElement(outputEl);
                                    } else {
                                        console.warn("Prism not available, skipping highlight");
                                    }
                                } catch (e) {
                                    console.error("Prism highlight error", e);
                                }
                            });
                            console.groupEnd();
                        }
                    };
                    tick();
                } catch (e) {
                    console.error("[HeroTypewriter:typing] error", e);
                    console.groupEnd();
                }
            }, 600);
        }
    };
}