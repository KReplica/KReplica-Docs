let kreplicaEditor = null;

function initKReplicaPlayground() {
    if (kreplicaEditor) {
        return;
    }

    require.config({paths: {vs: 'https://unpkg.com/monaco-editor@0.45.0/min/vs'}});
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

                const hiddenTextarea = document.querySelector('textarea[name="source"]');
                const initialCode = hiddenTextarea ? hiddenTextarea.value : '';
                const editorNode = document.getElementById('kreplica-editor');

                kreplicaEditor = monaco.editor.create(
                    editorNode,
                    {
                        value: initialCode,
                        language: 'kotlin',
                        automaticLayout: true,
                        theme: 'vs-dark',
                        minimap: {enabled: false},
                        folding: true,
                        scrollBeyondLastLine: false
                    }
                );

                const syncHeightToContent = () => {
                    if (!kreplicaEditor) return;
                    const contentHeight = kreplicaEditor.getContentHeight();
                    editorNode.style.height = contentHeight + 'px';
                    kreplicaEditor.layout();
                };

                monaco.languages.registerCompletionItemProvider('kotlin', {
                    triggerCharacters: ['@', '.'],
                    provideCompletionItems: function () {
                        return {suggestions: KREPLICA_COMPLETIONS};
                    }
                });

                kreplicaEditor.onDidContentSizeChange(syncHeightToContent);

                kreplicaEditor.onDidChangeModelContent(() => {
                    if (hiddenTextarea) {
                        hiddenTextarea.value = kreplicaEditor.getValue();
                    }
                });

                syncHeightToContent();
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

    const editorArea = document.querySelector('.playground-editor-area');
    if (!editorArea) return;

    const slug = editorArea.dataset.templateSlug;
    if (!slug) return;

    const url = `/playground/templates?template-select=${slug}`;
    htmx.ajax('GET', url, {target: '#editor-source-container', swap: 'innerHTML'});
}

function setupEventListeners() {
    const desktopResetButton = document.getElementById('reset-all-btn-desktop');
    desktopResetButton?.addEventListener('click', (e) => {
        if (!e.target.closest('.split-button-arrow')) {
            resetPlayground();
        }
    });
    document.getElementById('reset-all-dropdown-btn-desktop')?.addEventListener('click', resetPlayground);
    document.getElementById('clear-output-btn-desktop')?.addEventListener('click', clearPlaygroundOutput);

    const mobileResetButton = document.getElementById('reset-all-btn-mobile');
    mobileResetButton?.addEventListener('click', (e) => {
        if (!e.target.closest('.split-button-arrow')) {
            resetPlayground();
        }
    });
    document.getElementById('reset-all-dropdown-btn-mobile')?.addEventListener('click', resetPlayground);
    document.getElementById('clear-output-btn-mobile')?.addEventListener('click', clearPlaygroundOutput);
}

export function init() {
    initKReplicaPlayground();
    setupEventListeners();
}

export function getEditorInstance() {
    return kreplicaEditor;
}

export function disposeEditor() {
    if (kreplicaEditor) {
        kreplicaEditor.dispose();
        kreplicaEditor = null;
    }
    const node = document.getElementById('kreplica-editor');
    if (node) {
        node.style.height = '';
    }
}

export function setEditorValue(value) {
    if (kreplicaEditor) {
        kreplicaEditor.setValue(value);
    }
}

export {clearPlaygroundOutput};