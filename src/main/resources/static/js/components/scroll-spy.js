import {rafThrottle} from "../utils/throttle.js";

const SCROLL_OFFSET_PX = 85;

let activeScrollListener = null;
let activeResizeListener = null;
let isScrollSpyPaused = false;

export function pauseScrollSpy() {
    isScrollSpyPaused = true;
}

export function resumeScrollSpy() {
    isScrollSpyPaused = false;
}

export function initScrollSpy() {
    if (activeScrollListener) window.removeEventListener("scroll", activeScrollListener);
    if (activeResizeListener) window.removeEventListener("resize", activeResizeListener);

    const linksContainer = document.querySelector('[data-js-id="guide-sidebar-links"]');
    if (!linksContainer) {
        console.error("[ScrollSpy] Missing sidebar container");
        return;
    }

    const sections = Array.from(linksContainer.querySelectorAll('a[href^="#"]'))
        .map(a => document.getElementById(a.getAttribute("href").slice(1)))
        .filter(Boolean)
        .sort((a, b) => a.offsetTop - b.offsetTop);

    if (!sections.length) {
        console.error("[ScrollSpy] No sections found");
        return;
    }

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
            for (const section of sections) {
                if (section.offsetTop - SCROLL_OFFSET_PX <= scrollY) newActiveId = section.id;
                else break;
            }
        }

        if (!newActiveId) newActiveId = sections[0].id;

        if (newActiveId !== lastActiveId) {
            lastActiveId = newActiveId;
            document.body.dispatchEvent(new CustomEvent("section-active", {detail: {sectionId: newActiveId}}));
        }
    };

    activeScrollListener = rafThrottle(handleScroll);
    activeResizeListener = rafThrottle(initScrollSpy);

    window.addEventListener("scroll", activeScrollListener, {passive: true});
    window.addEventListener("resize", activeResizeListener);

    requestAnimationFrame(handleScroll);
}