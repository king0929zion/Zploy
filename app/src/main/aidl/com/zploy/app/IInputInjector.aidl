package com.zploy.app;

interface IInputInjector {
    boolean ping();
    boolean injectMotion(int action, long downTime, long eventTime, in int[] pointerIds, in float[] xs, in float[] ys);
    void destroy() = 16777114;
}
