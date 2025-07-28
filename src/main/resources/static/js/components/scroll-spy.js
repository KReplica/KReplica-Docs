import {rafThrottle} from "../utils/throttle";

let activeScrollListener = null;
let isScrollSpyPaused = false;

export function pauseScrollSpy() {
    isScrollSpyPaused = true;
}

export function resumeScrollSpy() {
    isScrollSpyPaused = false;
}

export function initScrollSpy() {
    if (activeScrollListener) {
        window.removeEventListener('scroll', activeScrollListener);
    }

    const linksContainer = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (!linksContainer) return;

    const sections = Array.from(linksContainer.querySelectorAll('a[href^="#"]'))
        .map(link => document.getElementById(link.getAttribute('href').substring(1)))
        .filter(section => section !== null);

    if (sections.length === 0) return;

    sections.sort((a, b) => a.offsetTop - b.offsetTop);

    let lastActiveId = null;

    const handleScroll = () => {
        if (isScrollSpyPaused) return;

        let newActiveId = null;
        const scrollBottom = Math.ceil(window.innerHeight + window.scrollY);
        const docHeight = document.documentElement.scrollHeight;

        if (scrollBottom >= docHeight) {
            newActiveId = sections[sections.length - 1].id;
        } else {
            const scrollY = window.scrollY;
            const offset = 85;
            for (const section of sections) {
                if (section.offsetTop - offset <= scrollY) {
                    newActiveId = section.id;
                } else {
                    break;
                }
            }
        }

        if (newActiveId === null && sections.length > 0) {
            newActiveId = sections[0].id;
        }

        if (newActiveId !== lastActiveId) {
            lastActiveId = newActiveId;
            const event = new CustomEvent('section-active', {detail: {sectionId: newActiveId}});
            document.body.dispatchEvent(event);
        }
    };

    activeScrollListener = rafThrottle(handleScroll);
    window.addEventListener('scroll', activeScrollListener);
    handleScroll();
}
