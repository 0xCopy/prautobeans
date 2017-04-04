package prauto.io;

import prauto.ann.Optional;
import prauto.ann.ProtoNumber;
import prauto.ann.ProtoOrigin;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static prauto.io.PackedPayloadUtil.readSize;
import static prauto.io.PackedPayloadUtil.writeSize;

/**
 * takes autobean-like interface. writes {non-optional,optional}{booelan,other}as bitset, then length-prefixed bytes.  Lists are length-prefixed payloads
 *
 * @param <ProtoMessage>
 */
public class PackedPayload<ProtoMessage> implements PackedPayloadUtil {

    /**
     * non-optional booleans.   always present.
     */
    public Collection<Method> bool = new TreeSet<>(METHOD_COMPARATOR);
    /**
     * optional variables that aren't bools. exist as part of the bitset above plus as n-byte values or ints to hold byte[] strings/blobs
     */
    public Collection<Method> opt = new TreeSet<>(METHOD_COMPARATOR);
    /**
     * always present before opt but after bitset.
     */
    public Collection<Method> nonOpt = new TreeSet<>(METHOD_COMPARATOR);
    /**
     * number of bits padded to 8, a constant per protobuf
     */
    private int bitsetLen;
    /**
     * too lazy/distrustful to bother with alignment/8
     */
    private int bitsetBytes;


    /**
     * this initializes the invariants that hold a serialized message.
     * <p/>
     * <p/>
     * [---len---][---bitset[-bools-][-optbools-]][---nonopt---][---opt---]
     *
     * @param theAutoBeanClass an-autobean like generated protobuf proxy interface
     */
    public PackedPayload(Class<ProtoMessage> theAutoBeanClass) {
         asList(theAutoBeanClass.getDeclaredMethods()).forEach(method -> {
            if (method.isAnnotationPresent(ProtoNumber.class)) {
                Collection<Method> l = boolean.class == method.getReturnType() || Boolean.class == method.getReturnType() ? bool : method.isAnnotationPresent(Optional.class) ? opt : nonOpt;
                l.add(method);
            }
        });

        this.bitsetLen = bool.size() + opt.size();
        BitSet bitSet = new BitSet(bitsetLen);
        bitSet.set(bitsetLen);
        this.bitsetBytes = bitSet.toByteArray().length;
        init(theAutoBeanClass);
    }

    private static void skim(ByteBuffer in, Map<Method, Object> values, Map<Method, Object> offsets, Method method, int position) {
        Class<?> returnType = method.getReturnType();
        if (VIEWGETTER.containsKey(returnType)) {
            values.put(method, VIEWGETTER.get(returnType).apply(in));
        } else if (returnType.isEnum()) {
            values.put(method, returnType.getEnumConstants()[in.getShort()]);
        } else {
            offsets.put(method, position);
            int anInt = readSize(in);
            in.position( in.position() + anInt);
        }
    }

    private void init(Class theAutoBean) {
        CODEX.putIfAbsent(theAutoBean, this);
    }

    public <C extends Class<ProtoMessage>> ProtoMessage get(C c, ByteBuffer in) {
        long size = readSize(in);
        byte[] bytes = new byte[bitsetBytes];
        in.get(bytes);
        BitSet bitSet = BitSet.valueOf(bytes);

        Map<Method, Object> values = new TreeMap<>(METHOD_COMPARATOR);
        Map<Method, Object> offsets = new TreeMap<>(METHOD_COMPARATOR);

        AtomicInteger bitCounter = new AtomicInteger(0);
        bool.forEach(method -> values.put(method, bitSet.get(bitCounter.getAndIncrement())));

        nonOpt.forEach(method -> skim(in, values, offsets, method, in.position()));

        //handle opt
        bitCounter.set(0);

        opt.forEach(method -> {
            if (bitSet.get(bool.size() + bitCounter.getAndIncrement()))
                skim(in, values, offsets, method, in.position());
            else
                values.put(method, null);
        });

        return (ProtoMessage) Proxy.newProxyInstance(c.getClassLoader(),
                new Class[]{c},
                (proxy, method, args) -> values.computeIfAbsent(method, k -> offsets.computeIfPresent(k, (k1, v) -> PackedPayload.this.registerMethod(in, method, (Integer) v))));

    }

    private Object registerMethod(ByteBuffer in, Method method, Integer v) {
        in.position(v);
        int size1 = readSize(in);
        int fin = in.position() + size1;

        Class returnType = method.getReturnType();
        Object r = null;
        if (returnType.isAnnotationPresent(ProtoOrigin.class))
            r = CODEX.computeIfAbsent(returnType, PackedPayload::new).get(returnType, in);
        else if (returnType.isAssignableFrom(List.class)) r = handleLists(in, method, size1, fin);
        return r;
    }

    private Object handleLists(ByteBuffer in, Method method, int size1, int fin) {
        Object r;//enums lack generic type parms. not sure why
        ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
        Class aClass = (Class) genericReturnType.getActualTypeArguments()[0];
        System.err.println("");

        if (VIEWSIZES.containsKey(aClass)) {
            r = handlePrimitiveList(in, size1, fin, aClass);
        } else {
            r = handleSequence(in, fin, aClass);
        }
        return r;
    }

    private Object handlePrimitiveList(ByteBuffer in, int size1, int fin, Class aClass) {
        Object r;
        r = new ReadOnlyBBList(aClass, VIEWSIZES.get(aClass), (ByteBuffer) in.slice().limit(size1));
        in.position(fin);
        return r;
    }

    private Object handleSequence(ByteBuffer in, int fin, Class aClass) {
        Object r;List<Object> objects = new ArrayList<>();
        r = objects;
        if (aClass.isEnum())
            while (in.position() < fin)
                objects.add(aClass.getEnumConstants()[in.getShort()]);
        else {
            PackedPayload packedPayload = CODEX.computeIfAbsent(aClass, PackedPayload::new);
            while (in.position() < fin) {
                Object o = packedPayload.get(aClass, in);
                objects.add(o);
            }
        }
        return r;
    }


    public void put(ProtoMessage proto, ByteBuffer out) {
        int begin = out.position();
        out.position(begin + 5);
        int fixup = out.position();
        BitSet bitSet = new BitSet(bitsetLen);
        if (0 < bitsetLen) bitSet.set(bitsetLen - 1);
        AtomicInteger c = new AtomicInteger(0);
        bool.forEach(method -> {
            try {
                bitSet.set(c.getAndIncrement(), Boolean.TRUE.equals(method.invoke(proto)));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        out.put(bitSet.toByteArray());
        nonOpt.forEach(method -> {
            try {
                Object invoke = method.invoke(proto);
                writeElement(out, invoke, method, null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        c.set(0);
        opt.forEach(method -> {
            try {

                Object invoke = method.invoke(proto);
                boolean b = null != invoke;
                bitSet.set(c.getAndIncrement() + bool.size(), b);
                if (b) writeElement(out, invoke, method, null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

        });
        ((ByteBuffer) out.duplicate().position(fixup)).put(bitSet.toByteArray());
        long size = out.position() - fixup;
        writeSize(out, begin, size);
        out.put(bitSet.toByteArray());
    }

    private void writeElement(ByteBuffer out, Object value, Method method, Class forcedClaz) {

        Class<?> returnType = null == forcedClaz ? null == method ? value.getClass() : method.getReturnType() : forcedClaz;
        if (VIEWSETTER.containsKey(returnType)) {

            VIEWSETTER.get(returnType).accept(out, value);

        } else if (returnType.isEnum()) {
            int ordinal = 0;
            try {
                ordinal = ((Enum) value).ordinal();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
            }
            out.putShort((short) ordinal);
        } else if (returnType.isAnnotationPresent(ProtoOrigin.class)) {
            PackedPayload packedPayload = CODEX.computeIfAbsent(returnType, aClass -> new PackedPayload(returnType));
            packedPayload.put(value, out);
        } else if (returnType.isAssignableFrom(List.class)) {
            int begin = out.position();
            out.position(begin + 5);
            int content = out.position();
            Class genericReturnType = (Class) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];

            List list = (List) value;
            if (VIEWSETTER.containsKey(genericReturnType))
                list.forEach(o -> VIEWSETTER.get(genericReturnType).accept(out, o));
            else if (genericReturnType.isEnum()) list.forEach(o -> out.putShort((short) (((Enum) o).ordinal() & 0xffff)));
            else if (genericReturnType.isAnnotationPresent(ProtoOrigin.class))
                list.forEach(o -> writeElement(out, o, null, genericReturnType));
            writeSize(out, begin, out.position() - content);
        }
    }


}
    