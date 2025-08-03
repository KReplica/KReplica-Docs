import {themeSwitcher} from './components/theme-switcher.js';

let activeController = null;

async function loadController(name, element) {
    try {
        const controllerModule = await import(`./controllers/${name}-controller.js`);
        activeController = controllerModule.default;
        if (activeController && typeof activeController.init === 'function') {
            activeController.init(element);
        }
    } catch (e) {
        console.error(`Failed to load controller: ${name}`, e);
        activeController = null;
    }
}

function destroyActiveController() {
    if (activeController && typeof activeController.destroy === 'function') {
        activeController.destroy();
    }
    activeController = null;
}

window.generateUniqueId = function () {
    if (crypto.randomUUID) return crypto.randomUUID();
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
};

function heroTypewriter() {
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


document.addEventListener('alpine:init', () => {
    Alpine.data('themeSwitcher', themeSwitcher);
    Alpine.data('heroTypewriter', heroTypewriter);
});

document.addEventListener('DOMContentLoaded', () => {
    const mainContent = document.querySelector('.main-content > [data-controller]');
    if (mainContent) {
        const controllerName = mainContent.dataset.controller;
        void loadController(controllerName, mainContent);
    }
});

document.body.addEventListener('htmx:configRequest', evt => {
    if (evt.detail.path === '/playground/compile' && window.KREPLICA_PLAYGROUND) {
        const editor = window.KREPLICA_PLAYGROUND.getEditorInstance();
        if (editor) evt.detail.parameters['source'] = editor.getValue();
    }
});

document.body.addEventListener('htmx:beforeSwap', (event) => {
    if (event.detail.target.classList.contains('main-content')) {
        destroyActiveController();
    }
});

document.body.addEventListener('htmx:afterSwap', e => {
    const mainContent = e.detail.target.querySelector('[data-controller]') || e.detail.target;
    if (mainContent && mainContent.dataset.controller) {
        void loadController(mainContent.dataset.controller, mainContent);
    }
    Prism.highlightAllUnder(e.detail.elt);
    if (e.detail.elt.id === 'playground-output') {
        window.dispatchEvent(new CustomEvent('output-ready'));
    }
});