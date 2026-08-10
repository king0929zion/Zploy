package com.zploy.app;

interface IInputInjector {
    boolean ping() = 1;
    boolean injectMotion(int action, long downTime, long eventTime, in int[] pointerIds, in float[] xs, in float[] ys) = 2;
    void destroy() = 16777114;
}
