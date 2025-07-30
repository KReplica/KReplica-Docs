let kreplicaEditor = null;
let cleanupFns = [];

function getHiddenTextarea() {
    return document.querySelector('textarea[name="source"]');
}

function initKReplicaPlayground() {
    if (kreplicaEditor) {
        return;
    }

    require.config({paths: {vs: 'https://unpkg.com/monaco-editor@0.52.2/min/vs'}});
    require(['vs/editor/editor.main'], function () {
        fetch('/api/completions')
            .then(response => response.json())
            .then(completionsData => {
                const KREPLICA_COMPLETIONS = completionsData.map(item => ({
                    label: item.label,
                    kind: monaco.languages.CompletionItemKind[item.kind] || monaco.languages.CompletionItemKind.Text,
                    insertText: item.insertText,
                    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                }));

                const hiddenTextareaEl = getHiddenTextarea();
                const initialCode = hiddenTextareaEl ? hiddenTextareaEl.value : '';
                const editorNode = document.getElementById('kreplica-editor');

                kreplicaEditor = monaco.editor.create(editorNode, {
                    value: initialCode,
                    language: 'kotlin',
                    automaticLayout: true,
                    theme: 'vs-dark',
                    minimap: {enabled: false},
                    folding: true,
                    scrollBeyondLastLine: false,
                    lineHeight: 20
                });

                monaco.languages.registerCompletionItemProvider('kotlin', {
                    triggerCharacters: ['@', '.'],
                    provideCompletionItems() {
                        return {suggestions: KREPLICA_COMPLETIONS};
                    }
                });

                kreplicaEditor.onDidChangeModelContent(() => {
                    const currentTextarea = getHiddenTextarea();
                    if (currentTextarea) {
                        currentTextarea.value = kreplicaEditor.getValue();
                    }
                });

                if (window.visualViewport) {
                    const onVVResize = () => {
                        if (kreplicaEditor) kreplicaEditor.layout();
                    };
                    window.visualViewport.addEventListener('resize', onVVResize);
                    cleanupFns.push(() => window.visualViewport.removeEventListener('resize', onVVResize));
                }

                const onVisibility = () => {
                    if (document.visibilityState === 'visible' && kreplicaEditor) {
                        kreplicaEditor.layout();
                    }
                };
                document.addEventListener('visibilitychange', onVisibility);
                cleanupFns.push(() => document.removeEventListener('visibilitychange', onVisibility));

                const column = document.querySelector('.playground-input-column');
                if (column) {
                    const mo = new MutationObserver(() => {
                        const node = document.getElementById('kreplica-editor');
                        if (node && node.offsetParent !== null && kreplicaEditor) {
                            kreplicaEditor.layout();
                        }
                    });
                    mo.observe(column, {attributes: true, attributeFilter: ['style', 'class']});
                    cleanupFns.push(() => mo.disconnect());
                }

                requestAnimationFrame(() => {
                    if (kreplicaEditor) kreplicaEditor.layout();
                });
            });
    });
}

function clearPlaygroundOutput() {
    const output = document.getElementById('playground-output');
    if (output) {
        output.innerHTML = '<div class="placeholder-text">Click "Run" to see the generated code.</div>';
    }
    window.dispatchEvent(new Event('clear-output'));
}

function resetPlayground() {
    clearPlaygroundOutput();
    const container = document.getElementById('editor-source-container');
    if (!container) return;
    const slugEl = container.querySelector('[data-template-slug]');
    const slug = slugEl ? slugEl.dataset.templateSlug : null;
    if (!slug) return;
    const url = `/playground/templates?template-select=${slug}`;
    htmx.ajax('GET', url, {target: '#editor-source-container', swap: 'innerHTML'});
}

function setupEventListeners() {
    const playgroundContainer = document.querySelector('.playground-container');
    if (!playgroundContainer) return;

    const onClick = (e) => {
        const actionTarget = e.target.closest('[data-action]');
        if (!actionTarget) return;

        const action = actionTarget.dataset.action;

        switch (action) {
            case 'run':
                break;
            case 'reset-all':
                if (!e.target.closest('.split-button-arrow')) {
                    resetPlayground();
                }
                break;
            case 'clear-output':
                clearPlaygroundOutput();
                break;
        }
    };

    playgroundContainer.addEventListener('click', onClick);
    cleanupFns.push(() => playgroundContainer.removeEventListener('click', onClick));
}

export function init() {
    initKReplicaPlayground();
    setupEventListeners();
}

export function getEditorInstance() {
    return kreplicaEditor;
}

export function disposeEditor() {
    cleanupFns.forEach(fn => {
        try {
            fn();
        } catch (_) {
        }
    });
    cleanupFns = [];
    if (kreplicaEditor) {
        const model = kreplicaEditor.getModel();
        if (model) {
            model.dispose();
        }
        kreplicaEditor.dispose();
        kreplicaEditor = null;
    }
}

export function setEditorValue(value) {
    if (kreplicaEditor) {
        kreplicaEditor.setValue(value);
        const currentTextarea = getHiddenTextarea();
        if (currentTextarea) {
            currentTextarea.value = value;
        }
        requestAnimationFrame(() => {
            if (kreplicaEditor) kreplicaEditor.layout();
        });
    }
}

export function setEditorModelFromSource(value) {
    if (!kreplicaEditor) return;
    const oldModel = kreplicaEditor.getModel();
    const newModel = monaco.editor.createModel(value, 'kotlin');
    kreplicaEditor.setModel(newModel);
    if (oldModel && oldModel !== newModel) {
        oldModel.dispose();
    }
    const currentTextarea = getHiddenTextarea();
    if (currentTextarea) {
        currentTextarea.value = value;
    }
    requestAnimationFrame(() => {
        if (kreplicaEditor) kreplicaEditor.layout();
    });
}

export {clearPlaygroundOutput};