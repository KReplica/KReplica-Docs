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

                const updateEditorHeight = () => {
                    if (kreplicaEditor) {
                        const contentHeight = kreplicaEditor.getContentHeight();
                        editorNode.style.height = `${contentHeight}px`;
                    }
                };

                monaco.languages.registerCompletionItemProvider('kotlin', {
                    triggerCharacters: ['@', '.'],
                    provideCompletionItems: function () {
                        return {suggestions: KREPLICA_COMPLETIONS};
                    }
                });

                kreplicaEditor.onDidChangeModelContent(() => {
                    if (hiddenTextarea) {
                        hiddenTextarea.value = kreplicaEditor.getValue();
                    }
                    updateEditorHeight();
                });

                updateEditorHeight();
            });
    });
}

function clearPlaygroundOutput() {
    const output = document.getElementById('playground-output');
    if (output) {
        output.innerHTML = '<div class="placeholder-text">Click "Run" to see the generated code.</div>';
    }
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

export function init() {
    if (document.getElementById('kreplica-editor')) {
        initKReplicaPlayground();
    }
}

export function getEditorInstance() {
    return kreplicaEditor;
}

export function disposeEditor() {
    if (kreplicaEditor) {
        kreplicaEditor.dispose();
        kreplicaEditor = null;
    }
}

export function getEditorValue() {
    return kreplicaEditor ? kreplicaEditor.getValue() : null;
}

export function setEditorValue(value) {
    if (kreplicaEditor) {
        kreplicaEditor.setValue(value);
    }
}

export {clearPlaygroundOutput, resetPlayground};