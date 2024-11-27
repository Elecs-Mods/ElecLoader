package nl.elec332.minecraft.loader.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LateObject<T> implements Consumer<T>, Supplier<T> {

    public LateObject() {
    }

    private volatile T object;

    public synchronized void set(final T object) {
        if (this.object == null) {
            synchronized(this) {
                if (this.object == null) {
                    this.object = Objects.requireNonNull(object);
                    return;
                }
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public T get() {
        T ret = this.object;
        if (ret == null) {
            synchronized(this) {
                ret = object;
                if (ret == null) {
                    throw new IllegalStateException("Object is null");
                }
            }
        }

        return ret;
    }

    @Override
    public void accept(T t) {
        this.set(t);
    }

}
