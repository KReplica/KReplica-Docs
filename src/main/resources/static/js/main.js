import {themeSwitcher} from './components/theme-switcher.js';
import * as playground from './pages/playground.js';

window.KREPLICA_PLAYGROUND = playground;

window.generateUniqueId = function () {
    if (crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
};

async function initializeApp() {
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const guide = await import('./pages/guide.js');
        guide.init();
    }

    if (document.getElementById('kreplica-editor')) {
        playground.init();
    }
}

document.addEventListener('alpine:init', () => {
    Alpine.data('themeSwitcher', themeSwitcher);
});

document.addEventListener('DOMContentLoaded', () => {
    void initializeApp();
});

document.body.addEventListener('htmx:afterSwap', (e) => {
    Prism.highlightAllUnder(e.detail.elt);
    void initializeApp();

    if (e.detail.target.id === 'playground-output') {
        window.dispatchEvent(new CustomEvent('output-ready'));
    }

    if (e.detail.target.id === 'editor-source-container') {
        import('./pages/playground.js').then(playground => {
            const editor = playground.getEditorInstance();
            if (editor) {
                const newSource = e.detail.target.querySelector('textarea[name="source"]').value;
                const alpineComponent = document.querySelector('.playground-container').__x;
                if (alpineComponent) {
                    alpineComponent.data.lastSubmittedSource = newSource;
                }
                if (playground.setEditorModelFromSource) {
                    playground.setEditorModelFromSource(newSource);
                } else {
                    playground.setEditorValue(newSource);
                }
            }
            if (playground.clearPlaygroundOutput) {
                playground.clearPlaygroundOutput();
            }
        });
    }
});

document.body.addEventListener('htmx:afterSettle', async () => {
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const {initScrollSpy} = await import('./components/scroll-spy.js');
        initScrollSpy();
    }
});

document.body.addEventListener('htmx:beforeSwap', (evt) => {
    const target = evt.detail.target;
    if (target && target.classList && target.classList.contains('main-content')) {
        import('./pages/playground.js').then(playground => {
            if (playground.getEditorInstance()) {
                playground.disposeEditor();
            }
        });
    }
});