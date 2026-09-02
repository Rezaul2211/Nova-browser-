package com.example.privacy

object AiAdDetector {
    val DOM_EXTRACTION_JS: String = """
        (function() {
            function getSimplifiedDOM(node, depth = 0, maxDepth = 10) {
                if (depth > maxDepth) return null;
                if (!node || node.nodeType !== Node.ELEMENT_NODE) return null;
                
                // Skip script, style, meta, head, svg, path, etc.
                const tag = node.tagName.toLowerCase();
                if (['script', 'style', 'meta', 'head', 'svg', 'path', 'g', 'link', 'noscript'].includes(tag)) return null;
                
                // Skip tiny elements
                const rect = node.getBoundingClientRect();
                if (rect.width < 10 || rect.height < 10) return null;
                
                let obj = {
                    tag: tag,
                    id: node.id || undefined,
                    cls: typeof node.className === 'string' ? node.className : undefined,
                };
                
                // Keep relevant attributes that might indicate ads
                if (node.hasAttribute('aria-label')) obj.ariaLabel = node.getAttribute('aria-label');
                if (node.hasAttribute('data-ad')) obj.dataAd = node.getAttribute('data-ad');
                if (node.hasAttribute('data-sponsor')) obj.dataSponsor = node.getAttribute('data-sponsor');
                if (node.hasAttribute('data-content')) obj.dataContent = node.getAttribute('data-content');
                if (node.hasAttribute('data-name')) obj.dataName = node.getAttribute('data-name');
                if (node.hasAttribute('role')) obj.role = node.getAttribute('role');
                if (tag === 'iframe' && node.hasAttribute('src')) obj.src = node.getAttribute('src');
                if (tag === 'img' && node.hasAttribute('src')) obj.src = node.getAttribute('src');
                
                let children = [];
                for (let i = 0; i < node.childNodes.length; i++) {
                    const child = node.childNodes[i];
                    if (child.nodeType === Node.ELEMENT_NODE) {
                        const childObj = getSimplifiedDOM(child, depth + 1, maxDepth);
                        if (childObj) {
                            children.push(childObj);
                        }
                    } else if (child.nodeType === Node.TEXT_NODE) {
                        const text = child.textContent.trim();
                        if (text.length > 0 && text.length < 100) {
                            obj.text = text;
                        }
                    }
                }
                if (children.length > 0) {
                    obj.children = children;
                }
                return obj;
            }
            
            try {
                return JSON.stringify(getSimplifiedDOM(document.body));
            } catch(e) {
                return "{}";
            }
        })();
    """.trimIndent()
}
