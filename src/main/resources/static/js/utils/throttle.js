export function rafThrottle(fn) {
    let ticking = false;
    return function () {
        if (ticking) return;
        ticking = true;
        requestAnimationFrame(() => {
            fn.apply(this, arguments);
            ticking = false;
        });
    };
}