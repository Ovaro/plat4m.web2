// SockJS still expects a browser-like global object in some bundling paths.
(window as typeof window & { global: Window & typeof globalThis }).global = window;
