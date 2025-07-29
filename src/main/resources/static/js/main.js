async function initializeApp() {
    if (document.querySelector('[data-js-id="guide-sidebar-links"]')) {
        const guide = await import('./pages/guide.js');
        guide.init();
    }

    if (document.getElementById('kreplica-editor')) {
        const playground = await import('./pages/playground.js');
        playground.init();
    }
}

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