import {rafThrottle} from "../utils/throttle.js";

class ScrollSpy {
    constructor(offsetPx = 85) {
        this.offsetPx = offsetPx;
        this.sections = [];
        this.lastActiveId = null;
        this.rafScroll = null;
        this.rafResize = null;
        this.isInitialized = false;
    }

    init() {
        if (this.isInitialized) {
            this.pause();
        }

        const linksContainer = document.querySelector('[data-js-id="guide-sidebar-links"]');
        if (!linksContainer) {
            console.error("[ScrollSpy] Missing sidebar container");
            return;
        }

        this.sections = Array.from(linksContainer.querySelectorAll('a[href^="#"]'))
            .map(a => document.getElementById(a.getAttribute("href").slice(1)))
            .filter(Boolean)
            .sort((a, b) => a.offsetTop - b.offsetTop);

        if (!this.sections.length) {
            console.error("[ScrollSpy] No sections found");
            return;
        }

        this.lastActiveId = null;
        this.rafScroll = rafThrottle(this.handleScroll.bind(this));
        this.rafResize = rafThrottle(this.init.bind(this));
        window.addEventListener("scroll", this.rafScroll, {passive: true});
        window.addEventListener("resize", this.rafResize);
        this.handleScroll();
        this.isInitialized = true;
    }

    handleScroll() {
        let newActiveId = null;
        const scrollBottom = Math.ceil(window.innerHeight + window.scrollY);
        const docHeight = document.documentElement.scrollHeight;

        if (scrollBottom >= docHeight) {
            newActiveId = this.sections[this.sections.length - 1].id;
        } else {
            const scrollY = window.scrollY;
            for (const section of this.sections) {
                if (section.offsetTop - this.offsetPx <= scrollY) {
                    newActiveId = section.id;
                } else {
                    break;
                }
            }
        }

        if (!newActiveId) {
            newActiveId = this.sections[0].id;
        }

        if (newActiveId !== this.lastActiveId) {
            this.lastActiveId = newActiveId;
            document.body.dispatchEvent(new CustomEvent("section-active", {detail: {sectionId: newActiveId}}));
        }
    }

    pause() {
        if (this.rafScroll) {
            window.removeEventListener("scroll", this.rafScroll);
        }
        if (this.rafResize) {
            window.removeEventListener("resize", this.rafResize);
        }
    }

    resume() {
        if (!this.isInitialized) {
            return;
        }
        if (this.rafScroll) {
            window.addEventListener("scroll", this.rafScroll, {passive: true});
        }
        if (this.rafResize) {
            window.addEventListener("resize", this.rafResize);
        }
    }

    destroy() {
        this.pause();
        this.sections = [];
        this.lastActiveId = null;
        this.rafScroll = null;
        this.rafResize = null;
        this.isInitialized = false;
    }
}

const scrollSpy = new ScrollSpy();

export function initScrollSpy() {
    scrollSpy.init();
}

export function pauseScrollSpy() {
    scrollSpy.pause();
}

export function resumeScrollSpy() {
    scrollSpy.resume();
}

export function destroyScrollSpy() {
    scrollSpy.destroy();
}