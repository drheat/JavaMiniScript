package com.catalinionescu.miniscript.utils.fastmath;

/**
 * Base class for arithmetic exceptions.
 * It is used for all the exceptions that have the semantics of the standard
 * {@link ArithmeticException}, but must also provide a localized
 * message.
 *
 * @since 3.0
 */
public class MathArithmeticException extends ArithmeticException
    implements ExceptionContextProvider {
    /** Serializable version Id. */
    private static final long serialVersionUID = -6024911025449780478L;
    /** Context. */
    private final ExceptionContext context;

    /**
     * Default constructor.
     */
    public MathArithmeticException() {
        context = new ExceptionContext(this);
        context.addMessage(LocalizedFormats.ARITHMETIC_EXCEPTION);
    }

    /**
     * Constructor with a specific message.
     *
     * @param pattern Message pattern providing the specific context of
     * the error.
     * @param args Arguments.
     */
    public MathArithmeticException(Localizable pattern,
                                   Object ... args) {
        context = new ExceptionContext(this);
        context.addMessage(pattern, args);
    }

    /** {@inheritDoc} */
    public ExceptionContext getContext() {
        return context;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return context.getMessage();
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedMessage() {
        return context.getLocalizedMessage();
    }
}