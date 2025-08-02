const REGEX = {
    ANNOTATION_CONTEXT: /@([\w.]+)\s*\(([\s\S]*)$/,
    DOT_TRIGGER: /@?([\w.]+)\.$/,
    ANNOTATION_TRIGGER: /@([\w.]*)$/,
    ARRAY_CONTEXT: /(\w+)\s*=\s*\[([^\]]*)$/,
    VALUE_CONTEXT: /(\w+)\s*=\s*([\w\s]*)$/,
};

function analyzeContext(model, position) {
    const textUntilPositionOnLine = model.getValueInRange({
        startLineNumber: position.lineNumber,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
    });

    const startLine = Math.max(1, position.lineNumber - 20);
    const textBlockBeforeCursor = model.getValueInRange({
        startLineNumber: startLine,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
    });

    const context = {
        inAnnotation: null,
        trigger: null,
        cursorLocation: null,
    };

    const dotMatch = textUntilPositionOnLine.match(REGEX.DOT_TRIGGER);
    const annotationTriggerMatch = textUntilPositionOnLine.match(REGEX.ANNOTATION_TRIGGER);

    if (dotMatch) {
        context.trigger = {char: '.', word: dotMatch[1]};
    } else if (annotationTriggerMatch) {
        context.trigger = {char: '@', partial: annotationTriggerMatch[1]};
    }

    const annotationMatch = textBlockBeforeCursor.match(REGEX.ANNOTATION_CONTEXT);
    if (annotationMatch) {
        context.inAnnotation = {
            name: annotationMatch[1],
            paramsText: annotationMatch[2] || '',
        };

        const arrayMatch = context.inAnnotation.paramsText.match(REGEX.ARRAY_CONTEXT);
        if (arrayMatch) {
            context.cursorLocation = 'ARRAY_VALUE';
            context.inAnnotation.paramName = arrayMatch[1];
        } else {
            const valueMatch = context.inAnnotation.paramsText.match(REGEX.VALUE_CONTEXT);
            if (valueMatch && valueMatch[2].trim() === "=") {
                context.cursorLocation = 'PARAMETER_VALUE';
                context.inAnnotation.paramName = valueMatch[1];
            } else {
                context.cursorLocation = 'PARAMETER_NAME';
            }
        }
    }

    return context;
}

function getDotSuggestions(context, languageModel) {
    const word = context.trigger.word;

    if (word === 'Replicate') {
        const suggestions = Object.keys(languageModel.annotations)
            .filter(key => key.startsWith('Replicate.'))
            .map(key => key.split('.')[1])
            .filter(Boolean)
            .map(name => {
                const fullKey = `Replicate.${name}`;
                const annotationDef = languageModel.annotations[fullKey];
                let insertText = name;
                if (annotationDef?.insertText) {
                    insertText = annotationDef.insertText.replace(/^Replicate\./, '');
                }
                return {
                    label: name,
                    kind: monaco.languages.CompletionItemKind.Module,
                    insertText: insertText,
                    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                };
            });
        const uniqueSuggestions = [...new Set(suggestions.map(s => s.label))].map(label => suggestions.find(s => s.label === label));
        return {suggestions: uniqueSuggestions};
    }

    const enumDef = languageModel.enums[word];
    if (enumDef) {
        return {
            suggestions: enumDef.members.map(member => ({
                label: member,
                kind: monaco.languages.CompletionItemKind.EnumMember,
                insertText: member,
            }))
        };
    }

    const typeDef = languageModel.types[word];
    if (typeDef) {
        return {
            suggestions: typeDef.members.map(member => ({
                label: member.name,
                kind: monaco.languages.CompletionItemKind.Method,
                detail: member.detail,
                documentation: member.documentation,
                insertText: member.insertText,
                insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            }))
        };
    }

    return {suggestions: []};
}

function getAnnotationSuggestions(context, languageModel) {
    const namespaces = new Set();
    const standaloneAnnotations = [];

    const processAnnotation = (key, val) => {
        if (key.includes('.')) {
            namespaces.add(key.split('.')[0]);
        } else {
            standaloneAnnotations.push({
                label: `@${key}`,
                kind: monaco.languages.CompletionItemKind.Function,
                documentation: val.documentation,
                insertText: `@${val.insertText}`,
                insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            });
        }
    };

    Object.entries(languageModel.annotations).forEach(([key, val]) => processAnnotation(key, val));
    Object.entries(languageModel.commonAnnotations).forEach(([key, val]) => processAnnotation(key, val));

    const namespaceSuggestions = Array.from(namespaces).map(ns => ({
        label: `@${ns}`,
        kind: monaco.languages.CompletionItemKind.Module,
        insertText: `@${ns}.$0`,
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        command: {id: 'editor.action.triggerSuggest'}
    }));

    return {suggestions: [...namespaceSuggestions, ...standaloneAnnotations]};
}

function getParameterNameSuggestions(context, languageModel) {
    const annotationDef = languageModel.annotations[context.inAnnotation.name];
    if (!annotationDef?.parameters) {
        return {suggestions: []};
    }

    const typedParams = context.inAnnotation.paramsText.split(',').map(p => p.split('=')[0].trim());
    const suggestions = Object.entries(annotationDef.parameters)
        .filter(([paramName]) => !typedParams.includes(paramName))
        .map(([paramName, paramInfo]) => ({
            label: paramName,
            kind: monaco.languages.CompletionItemKind.Property,
            detail: paramInfo.detail,
            documentation: paramInfo.documentation,
            insertText: paramInfo.insertText,
            sortText: paramInfo.sortText || "99",
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        }));

    return {suggestions};
}

function getParameterValueSuggestions(context, languageModel) {
    const paramDef = languageModel.annotations[context.inAnnotation.name]?.parameters?.[context.inAnnotation.paramName];
    if (!paramDef?.valueContext) {
        return {suggestions: []};
    }

    const enumDef = languageModel.enums[paramDef.valueContext];
    if (!enumDef) {
        return {suggestions: []};
    }

    const suggestions = enumDef.members.map(member => ({
        label: `${paramDef.valueContext}.${member}`,
        kind: monaco.languages.CompletionItemKind.EnumMember,
        insertText: `${paramDef.valueContext}.${member}`,
    }));

    return {suggestions};
}

function getArrayValueSuggestions(context, languageModel) {
    const paramDef = languageModel.annotations[context.inAnnotation.name]?.parameters?.[context.inAnnotation.paramName];
    if (!paramDef) {
        return {suggestions: []};
    }

    if (paramDef.valueContext) {
        const enumDef = languageModel.enums[paramDef.valueContext];
        if (enumDef) {
            return {
                suggestions: enumDef.members.map(member => ({
                    label: `${paramDef.valueContext}.${member}`,
                    kind: monaco.languages.CompletionItemKind.EnumMember,
                    insertText: `${paramDef.valueContext}.${member}`,
                }))
            };
        }
    }

    if (paramDef.valueType === 'KClass' && languageModel.kclasses) {
        return {
            suggestions: Object.values(languageModel.kclasses).map(kclassInfo => ({
                label: kclassInfo.insertText,
                kind: monaco.languages.CompletionItemKind.Class,
                documentation: kclassInfo.documentation,
                insertText: kclassInfo.insertText,
            }))
        };
    }

    return {suggestions: []};
}

export function getCompletionProvider(languageModel) {
    if (!languageModel) {
        return {provideCompletionItems: () => ({suggestions: []})};
    }

    return {
        triggerCharacters: ['@', '.', '(', ',', ' ', '['],

        provideCompletionItems: (model, position) => {
            const context = analyzeContext(model, position);

            if (context.trigger?.char === '.') {
                return getDotSuggestions(context, languageModel);
            }

            if (context.inAnnotation) {
                if (context.cursorLocation === 'ARRAY_VALUE') {
                    return getArrayValueSuggestions(context, languageModel);
                }
                if (context.cursorLocation === 'PARAMETER_VALUE') {
                    return getParameterValueSuggestions(context, languageModel);
                }
                if (context.cursorLocation === 'PARAMETER_NAME') {
                    return getParameterNameSuggestions(context, languageModel);
                }
                return {suggestions: []};
            }

            if (context.trigger?.char === '@') {
                return getAnnotationSuggestions(context, languageModel);
            }

            return {suggestions: []};
        },
    };
}