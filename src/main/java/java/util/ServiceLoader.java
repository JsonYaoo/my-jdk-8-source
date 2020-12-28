/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 20201228
 * A. 一个简单的服务提供者加载工具。
 * B. 服务是一组著名的接口，并且（通常抽象）类。 服务提供者是服务的特定实现。 提供程序中的类通常实现接口，并子类化服务本身中定义的类。 服务提供者可以扩展的形式安装在Java平台
 *    的实现中，也就是说，将jar文件放置在任何常用扩展目录中。 也可以通过将提供者添加到应用程序的类路径或通过其他一些特定于平台的方式来使提供者可用。
 * C. 出于加载的目的，服务由单一类型表示，即单一接口或抽象类。 （可以使用一个具体的类，但是不建议这样做。）给定服务的提供者包含一个或多个具体类，这些具体类使用该提供者特定的
 *    数据和代码扩展该服务类型。 提供者类通常不是整个提供者本身，而是包含足够信息以决定提供者是否能够满足特定请求以及可以按需创建实际提供者的代码的代理。 提供者类的细节往往
 *    是高度特定于服务的； 没有单个类或接口可能会统一它们，因此此处未定义此类。 此功能强制执行的唯一要求是，提供程序类必须具有零参数构造函数，以便可以在加载期间实例化它们。
 * D. 通过在资源目录META-INF / services中放置提供者配置文件来标识服务提供者。该文件的名称是完全限定的服务类型的二进制名称。 该文件包含一个具体的提供程序类的标准二进制名称列表，
 *    每行一个。 每个名称周围的空格和制表符以及空白行将被忽略。 注释字符为“＃”（“＆＃92; u0023”，数字符号）； 在每一行中，第一个注释字符之后的所有字符都将被忽略。
 *    该文件必须使用UTF-8编码。
 * E. 如果一个特定的具体提供程序类在多个配置文件中被命名，或者在同一配置文件中被多次命名，则重复项将被忽略。 命名特定提供程序的配置文件不必与提供程序本身位于同一jar文件或
 *    其他分发单元中。 该提供程序必须可以从最初查询定位配置文件的同一类加载程序进行访问； 请注意，这不一定是实际从中加载文件的类加载器。
 * F. 提供者的位置是懒惰的，即按需实例化。 服务加载器维护到目前为止已加载的提供者的缓存。 每次调用{@link #iterator iterator}方法时，都会返回一个迭代器，该迭代器
 *    首先以实例化顺序生成高速缓存的所有元素，然后懒惰地定位和实例化任何剩余的提供程序，依次将每个提供程序添加到高速缓存中。 可以通过{@link #reload reload}方法清除缓存。
 * G. 服务加载程序始终在调用方的安全上下文中执行。 受信任的系统代码通常应从特权安全上下文中调用此类中的方法以及它们返回的迭代器的方法。
 * H. 此类的实例不适用于多个并发线程。
 * I. 除非另有说明，否则将null参数传递给此类中的任何方法都将引发{@link NullPointerException}。
 * J. 示例假设我们有一个服务类型com.example.CodecSet，它旨在表示某种协议的编码器/解码器对的集合。 在这种情况下，它是一个具有两个抽象方法的抽象类：
 *      public abstract Encoder getEncoder(String encodingName);
 *      public abstract Decoder getDecoder(String encodingName);
 * K. 每个方法都返回一个适当的对象，如果提供者不支持给定的编码，则返回null。 典型的提供程序支持多种编码。
 * L. 如果com.example.impl.StandardCodecs是CodecSet服务的实现，则其jar文件还包含一个名为META-INF / services / com.example.CodecSet的文件。
 * M. 此文件包含单行：com.example.impl.StandardCodecs＃标准编解码器
 * N. CodecSet类在初始化时创建并保存一个服务实例：private static ServiceLoader<CodecSet> codecSetLoader = ServiceLoader.load（CodecSet.class）;
 * O. 为了找到给定编码名称的编码器，它定义了一个静态工厂方法，该方法迭代已知和可用的提供者，仅在找到合适的编码器或用尽提供者时返回:
 *      public static Encoder getEncoder(String encodingName) {
 *          for (CodecSet cp : codecSetLoader) {
 *              Encoder enc = cp.getEncoder(encodingName);
 *              if (enc != null)
 *                  return enc;
 *              }
 *          return null;
 *      }
 * P. 类似地定义了getDecoder方法。
 * Q. 使用说明: 如果用于提供程序加载的类加载程序的类路径包括远程网络URL，则将在搜索提供程序配置文件的过程中取消引用这些URL。
 * R. 此活动是正常的，尽管它可能会导致在Web服务器日志中创建令人费解的条目。 但是，如果未正确配置Web服务器，则此活动可能导致提供商加载算法错误地失败。
 * S. 当请求的资源不存在时，Web服务器应返回HTTP 404（未找到）响应。 但是，在某些情况下，有时会将Web服务器错误地配置为返回HTTP 200（OK）响应以及有用的HTML错误页面。
 *    当此类尝试将HTML页面解析为提供程序配置文件时，这将引发{@link ServiceConfigurationError}。 解决此问题的最佳方法是修复配置错误的Web服务器，以返回正确的响应代码
 *    （HTTP 404）和HTML错误页面。
 */
/**
 * A.
 * A simple service-provider loading facility.
 *
 * B.
 * <p> A <i>service</i> is a well-known set of interfaces and (usually
 * abstract) classes.  A <i>service provider</i> is a specific implementation
 * of a service.  The classes in a provider typically implement the interfaces
 * and subclass the classes defined in the service itself.  Service providers
 * can be installed in an implementation of the Java platform in the form of
 * extensions, that is, jar files placed into any of the usual extension
 * directories.  Providers can also be made available by adding them to the
 * application's class path or by some other platform-specific means.
 *
 * C.
 * <p> For the purpose of loading, a service is represented by a single type,
 * that is, a single interface or abstract class.  (A concrete class can be
 * used, but this is not recommended.)  A provider of a given service contains
 * one or more concrete classes that extend this <i>service type</i> with data
 * and code specific to the provider.  The <i>provider class</i> is typically
 * not the entire provider itself but rather a proxy which contains enough
 * information to decide whether the provider is able to satisfy a particular
 * request together with code that can create the actual provider on demand.
 * The details of provider classes tend to be highly service-specific; no
 * single class or interface could possibly unify them, so no such type is
 * defined here.  The only requirement enforced by this facility is that
 * provider classes must have a zero-argument constructor so that they can be
 * instantiated during loading.
 *
 * D.
 * <p><a name="format"> A service provider is identified by placing a
 * <i>provider-configuration file</i> in the resource directory
 * <tt>META-INF/services</tt>.</a>  The file's name is the fully-qualified <a
 * href="../lang/ClassLoader.html#name">binary name</a> of the service's type.
 * The file contains a list of fully-qualified binary names of concrete
 * provider classes, one per line.  Space and tab characters surrounding each
 * name, as well as blank lines, are ignored.  The comment character is
 * <tt>'#'</tt> (<tt>'&#92;u0023'</tt>,
 * <font style="font-size:smaller;">NUMBER SIGN</font>); on
 * each line all characters following the first comment character are ignored.
 * The file must be encoded in UTF-8.
 *
 * E.
 * <p> If a particular concrete provider class is named in more than one
 * configuration file, or is named in the same configuration file more than
 * once, then the duplicates are ignored.  The configuration file naming a
 * particular provider need not be in the same jar file or other distribution
 * unit as the provider itself.  The provider must be accessible from the same
 * class loader that was initially queried to locate the configuration file;
 * note that this is not necessarily the class loader from which the file was
 * actually loaded.
 *
 * F.
 * <p> Providers are located and instantiated lazily, that is, on demand.  A
 * service loader maintains a cache of the providers that have been loaded so
 * far.  Each invocation of the {@link #iterator iterator} method returns an
 * iterator that first yields all of the elements of the cache, in
 * instantiation order, and then lazily locates and instantiates any remaining
 * providers, adding each one to the cache in turn.  The cache can be cleared
 * via the {@link #reload reload} method.
 *
 * G.
 * <p> Service loaders always execute in the security context of the caller.
 * Trusted system code should typically invoke the methods in this class, and
 * the methods of the iterators which they return, from within a privileged
 * security context.
 *
 * H.
 * <p> Instances of this class are not safe for use by multiple concurrent
 * threads.
 *
 * I.
 * <p> Unless otherwise specified, passing a <tt>null</tt> argument to any
 * method in this class will cause a {@link NullPointerException} to be thrown.
 *
 * J.
 * <p><span style="font-weight: bold; padding-right: 1em">Example</span>
 * Suppose we have a service type <tt>com.example.CodecSet</tt> which is
 * intended to represent sets of encoder/decoder pairs for some protocol.  In
 * this case it is an abstract class with two abstract methods:
 *
 * <blockquote><pre>
 * public abstract Encoder getEncoder(String encodingName);
 * public abstract Decoder getDecoder(String encodingName);</pre></blockquote>
 *
 * K.
 * Each method returns an appropriate object or <tt>null</tt> if the provider
 * does not support the given encoding.  Typical providers support more than
 * one encoding.
 *
 * L.
 * <p> If <tt>com.example.impl.StandardCodecs</tt> is an implementation of the
 * <tt>CodecSet</tt> service then its jar file also contains a file named
 *
 * <blockquote><pre>
 * META-INF/services/com.example.CodecSet</pre></blockquote>
 *
 * M.
 * <p> This file contains the single line:
 *
 * <blockquote><pre>
 * com.example.impl.StandardCodecs    # Standard codecs</pre></blockquote>
 *
 * N.
 * <p> The <tt>CodecSet</tt> class creates and saves a single service instance
 * at initialization:
 *
 * <blockquote><pre>
 * private static ServiceLoader&lt;CodecSet&gt; codecSetLoader
 *     = ServiceLoader.load(CodecSet.class);</pre></blockquote>
 *
 * O.
 * <p> To locate an encoder for a given encoding name it defines a static
 * factory method which iterates through the known and available providers,
 * returning only when it has located a suitable encoder or has run out of
 * providers.
 *
 * <blockquote><pre>
 * public static Encoder getEncoder(String encodingName) {
 *     for (CodecSet cp : codecSetLoader) {
 *         Encoder enc = cp.getEncoder(encodingName);
 *         if (enc != null)
 *             return enc;
 *     }
 *     return null;
 * }</pre></blockquote>
 *
 * P.
 * <p> A <tt>getDecoder</tt> method is defined similarly.
 *
 * Q.
 * <p><span style="font-weight: bold; padding-right: 1em">Usage Note</span> If
 * the class path of a class loader that is used for provider loading includes
 * remote network URLs then those URLs will be dereferenced in the process of
 * searching for provider-configuration files.
 *
 * R.
 * <p> This activity is normal, although it may cause puzzling entries to be
 * created in web-server logs.  If a web server is not configured correctly,
 * however, then this activity may cause the provider-loading algorithm to fail
 * spuriously.
 *
 * S.
 * <p> A web server should return an HTTP 404 (Not Found) response when a
 * requested resource does not exist.  Sometimes, however, web servers are
 * erroneously configured to return an HTTP 200 (OK) response along with a
 * helpful HTML error page in such cases.  This will cause a {@link
 * ServiceConfigurationError} to be thrown when this class attempts to parse
 * the HTML page as a provider-configuration file.  The best solution to this
 * problem is to fix the misconfigured web server to return the correct
 * response code (HTTP 404) along with the HTML error page.
 *
 * @param  <S>
 *         The type of the service to be loaded by this loader
 *
 * @author Mark Reinhold
 * @since 1.6
 */
// 20201228 一个简单的服务提供者加载工具: SPI服务加载器, 通过在资源目录META-INF / services中放置提供者配置文件来标识服务提供者, 重复项将被忽略, 此类的实例不适用于多个并发线程
public final class ServiceLoader<S> implements Iterable<S>
{

    private static final String PREFIX = "META-INF/services/";

    // The class or interface representing the service being loaded
    private final Class<S> service;

    // The class loader used to locate, load, and instantiate providers
    private final ClassLoader loader;

    // The access control context taken when the ServiceLoader is created
    private final AccessControlContext acc;

    // Cached providers, in instantiation order
    private LinkedHashMap<String,S> providers = new LinkedHashMap<>();

    // The current lazy-lookup iterator
    private LazyIterator lookupIterator;

    /**
     * Clear this loader's provider cache so that all providers will be
     * reloaded.
     *
     * <p> After invoking this method, subsequent invocations of the {@link
     * #iterator() iterator} method will lazily look up and instantiate
     * providers from scratch, just as is done by a newly-created loader.
     *
     * <p> This method is intended for use in situations in which new providers
     * can be installed into a running Java virtual machine.
     */
    public void reload() {
        providers.clear();
        lookupIterator = new LazyIterator(service, loader);
    }

    private ServiceLoader(Class<S> svc, ClassLoader cl) {
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload();
    }

    private static void fail(Class<?> service, String msg, Throwable cause)
        throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg,
                                            cause);
    }

    private static void fail(Class<?> service, String msg)
        throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    private static void fail(Class<?> service, URL u, int line, String msg)
        throws ServiceConfigurationError
    {
        fail(service, u + ":" + line + ": " + msg);
    }

    // Parse a single line from the given configuration file, adding the name
    // on the line to the names list.
    //
    private int parseLine(Class<?> service, URL u, BufferedReader r, int lc,
                          List<String> names)
        throws IOException, ServiceConfigurationError
    {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf('#');
        if (ci >= 0) ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0))
                fail(service, u, lc, "Illegal configuration-file syntax");
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            if (!providers.containsKey(ln) && !names.contains(ln))
                names.add(ln);
        }
        return lc + 1;
    }

    // Parse the content of the given URL as a provider-configuration file.
    //
    // @param  service
    //         The service type for which providers are being sought;
    //         used to construct error detail strings
    //
    // @param  u
    //         The URL naming the configuration file to be parsed
    //
    // @return A (possibly empty) iterator that will yield the provider-class
    //         names in the given configuration file that are not yet members
    //         of the returned set
    //
    // @throws ServiceConfigurationError
    //         If an I/O error occurs while reading from the given URL, or
    //         if a configuration-file format error is detected
    //
    private Iterator<String> parse(Class<?> service, URL u)
        throws ServiceConfigurationError
    {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<>();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, u, r, lc, names)) >= 0);
        } catch (IOException x) {
            fail(service, "Error reading configuration file", x);
        } finally {
            try {
                if (r != null) r.close();
                if (in != null) in.close();
            } catch (IOException y) {
                fail(service, "Error closing configuration file", y);
            }
        }
        return names.iterator();
    }

    // Private inner class implementing fully-lazy provider lookup
    //
    private class LazyIterator
        implements Iterator<S>
    {

        Class<S> service;
        ClassLoader loader;
        Enumeration<URL> configs = null;
        Iterator<String> pending = null;
        String nextName = null;

        private LazyIterator(Class<S> service, ClassLoader loader) {
            this.service = service;
            this.loader = loader;
        }

        private boolean hasNextService() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }

        private S nextService() {
            if (!hasNextService())
                throw new NoSuchElementException();
            String cn = nextName;
            nextName = null;
            Class<?> c = null;
            try {
                c = Class.forName(cn, false, loader);
            } catch (ClassNotFoundException x) {
                fail(service,
                     "Provider " + cn + " not found");
            }
            if (!service.isAssignableFrom(c)) {
                fail(service,
                     "Provider " + cn  + " not a subtype");
            }
            try {
                S p = service.cast(c.newInstance());
                providers.put(cn, p);
                return p;
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated",
                     x);
            }
            throw new Error();          // This cannot happen
        }

        public boolean hasNext() {
            if (acc == null) {
                return hasNextService();
            } else {
                PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
                    public Boolean run() { return hasNextService(); }
                };
                return AccessController.doPrivileged(action, acc);
            }
        }

        public S next() {
            if (acc == null) {
                return nextService();
            } else {
                PrivilegedAction<S> action = new PrivilegedAction<S>() {
                    public S run() { return nextService(); }
                };
                return AccessController.doPrivileged(action, acc);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * 20201228
     * A. 延迟加载此加载程序服务的可用提供程序。
     * B. 此方法返回的迭代器首先以实例化顺序生成提供程序缓存的所有元素。 然后，它懒惰地加载并实例化任何剩余的提供程序，依次将每个提供程序添加到缓存中。
     * C. 为了懒惰，解析可用的提供程序配置文件和实例化提供程序的实际工作必须由迭代器本身完成。 因此，如果提供者配置文件违反了指定格式，或者其违反了指定格式，则其
     *    {@link java.util.Iterator＃hasNext hasNext}和{@link java.util.Iterator＃next next}方法可以抛出{@link ServiceConfigurationError}。
     *    命名无法找到和实例化的提供程序类，或者实例化该类的结果不能分配给服务类型，或者在定位和实例化下一个提供程序时引发任何其他类型的异常或错误。 要编写健壮的代码，
     *    只需在使用服务迭代器时捕获{@link ServiceConfigurationError}。
     * D. 如果抛出这样的错误，则迭代器的后续调用将尽最大努力查找并实例化下一个可用的提供程序，但通常不能保证这种恢复。 设计说明在这些情况下引发错误可能看起来很极端。
     *    此行为的基本原理是，格式错误的提供者配置文件（如格式错误的类文件）指示Java虚拟机的配置或使用方式存在严重问题。 因此，最好抛出一个错误而不是尝试恢复，
     *    或者更糟糕的是，静默地失败。
     * E. 此方法返回的迭代器不支持删除。 调用其{@link java.util.Iterator＃remove（）remove}方法将导致抛出{@link UnsupportedOperationException}。
     * F. 将提供程序添加到缓存时，{@link #iterator }迭代器按照{@link java.lang.ClassLoader＃getResources（java.lang.String）ClassLoader.getResources（String）}
     *    方法找到服务的顺序处理资源。 配置文件。
     */
    /**
     * A.
     * Lazily loads the available providers of this loader's service.
     *
     * B.
     * <p> The iterator returned by this method first yields all of the
     * elements of the provider cache, in instantiation order.  It then lazily
     * loads and instantiates any remaining providers, adding each one to the
     * cache in turn.
     *
     * C.
     * <p> To achieve laziness the actual work of parsing the available
     * provider-configuration files and instantiating providers must be done by
     * the iterator itself.  Its {@link java.util.Iterator#hasNext hasNext} and
     * {@link java.util.Iterator#next next} methods can therefore throw a
     * {@link ServiceConfigurationError} if a provider-configuration file
     * violates the specified format, or if it names a provider class that
     * cannot be found and instantiated, or if the result of instantiating the
     * class is not assignable to the service type, or if any other kind of
     * exception or error is thrown as the next provider is located and
     * instantiated.  To write robust code it is only necessary to catch {@link
     * ServiceConfigurationError} when using a service iterator.
     *
     * D.
     * <p> If such an error is thrown then subsequent invocations of the
     * iterator will make a best effort to locate and instantiate the next
     * available provider, but in general such recovery cannot be guaranteed.
     *
     * <blockquote style="font-size: smaller; line-height: 1.2"><span
     * style="padding-right: 1em; font-weight: bold">Design Note</span>
     * Throwing an error in these cases may seem extreme.  The rationale for
     * this behavior is that a malformed provider-configuration file, like a
     * malformed class file, indicates a serious problem with the way the Java
     * virtual machine is configured or is being used.  As such it is
     * preferable to throw an error rather than try to recover or, even worse,
     * fail silently.</blockquote>
     *
     * E.
     * <p> The iterator returned by this method does not support removal.
     * Invoking its {@link java.util.Iterator#remove() remove} method will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * F.
     * @implNote When adding providers to the cache, the {@link #iterator
     * Iterator} processes resources in the order that the {@link
     * java.lang.ClassLoader#getResources(java.lang.String)
     * ClassLoader.getResources(String)} method finds the service configuration
     * files.
     *
     * @return  An iterator that lazily loads providers for this loader's
     *          service
     */
    // 20201228 延迟加载此加载程序服务的可用提供程序
    public Iterator<S> iterator() {
        return new Iterator<S>() {

            Iterator<Map.Entry<String,S>> knownProviders
                = providers.entrySet().iterator();

            public boolean hasNext() {
                if (knownProviders.hasNext())
                    return true;
                return lookupIterator.hasNext();
            }

            public S next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();
                return lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * // 20201228 为给定的服务类型和类加载器创建一个新的服务加载器。
     * Creates a new service loader for the given service type and class
     * loader.
     *
     * @param  <S> the class of the service type
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @param  loader
     *         The class loader to be used to load provider-configuration files
     *         and provider classes, or <tt>null</tt> if the system class
     *         loader (or, failing that, the bootstrap class loader) is to be
     *         used
     *
     * @return A new service loader
     */
    // 20201228 为给定的服务类型和类加载器创建一个新的服务加载器。
    public static <S> ServiceLoader<S> load(Class<S> service,
                                            ClassLoader loader)
    {
        return new ServiceLoader<>(service, loader);
    }

    /**
     * 20201228
     * A. 使用当前线程的{@linkplain java.lang.Thread＃getContextClassLoader上下文类加载器}，为给定的服务类型创建一个新的服务加载器。
     * B. 以ServiceLoader.load（service）形式调用此便捷方法等效于ServiceLoader.load（service，Thread.currentThread（）。getContextClassLoader（））
     */
    /**
     * A.
     * Creates a new service loader for the given service type, using the
     * current thread's {@linkplain java.lang.Thread#getContextClassLoader
     * context class loader}.
     *
     * B.
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>)</pre></blockquote>
     *
     * is equivalent to
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>,
     *                    Thread.currentThread().getContextClassLoader())</pre></blockquote>
     *
     * @param  <S> the class of the service type
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     */
    // 20201228 使用当前线程的{@linkplain java.lang.Thread＃getContextClassLoader上下文类加载器}，为给定的服务类型创建一个新的服务加载器
    public static <S> ServiceLoader<S> load(Class<S> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceLoader.load(service, cl);
    }

    /**
     * Creates a new service loader for the given service type, using the
     * extension class loader.
     *
     * <p> This convenience method simply locates the extension class loader,
     * call it <tt><i>extClassLoader</i></tt>, and then returns
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>, <i>extClassLoader</i>)</pre></blockquote>
     *
     * <p> If the extension class loader cannot be found then the system class
     * loader is used; if there is no system class loader then the bootstrap
     * class loader is used.
     *
     * <p> This method is intended for use when only installed providers are
     * desired.  The resulting service will only find and load providers that
     * have been installed into the current Java virtual machine; providers on
     * the application's class path will be ignored.
     *
     * @param  <S> the class of the service type
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     */
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return ServiceLoader.load(service, prev);
    }

    /**
     * Returns a string describing this service.
     *
     * @return  A descriptive string
     */
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }

}
