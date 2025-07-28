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

document.addEventListener('DOMContentLoaded', initializeApp);

document.body.addEventListener('htmx:afterSwap', (e) => {
    Prism.highlightAllUnder(e.detail.elt);
    initializeApp();

    if (e.detail.target.id === 'editor-source-container') {
        import('./pages/playground.js').then(playground => {
            const editor = playground.getEditorInstance();
            if (editor) {
                const newSource = e.detail.target.querySelector('textarea[name="source"]').value;
                playground.setEditorValue(newSource);
            }
            if (playground.clearPlaygroundOutput) {
                playground.clearPlaygroundOutput();
            }
        });
    }
});

document.body.addEventListener('htmx:beforeSwap', (evt) => {
    if (evt.detail.target.id !== 'playground-output') {
        import('./pages/playground.js').then(playground => {
            if (playground.getEditorInstance()) {
                playground.disposeEditor();
            }
        });
    }
});