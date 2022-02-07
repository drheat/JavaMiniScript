package com.catalinionescu.miniscript.utils.fastmath;

/**
 * Exception to be thrown when two dimensions differ.
 *
 * @since 2.2
 */
public class DimensionMismatchException extends MathIllegalNumberException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -8415396756375798143L;
    /** Correct dimension. */
    private final int dimension;

    /**
     * Construct an exception from the mismatched dimensions.
     *
     * @param specific Specific context information pattern.
     * @param wrong Wrong dimension.
     * @param expected Expected dimension.
     */
    public DimensionMismatchException(Localizable specific,
                                      int wrong,
                                      int expected) {
        super(specific, Integer.valueOf(wrong), Integer.valueOf(expected));
        dimension = expected;
    }

    /**
     * Construct an exception from the mismatched dimensions.
     *
     * @param wrong Wrong dimension.
     * @param expected Expected dimension.
     */
    public DimensionMismatchException(int wrong,
                                      int expected) {
        this(LocalizedFormats.DIMENSIONS_MISMATCH_SIMPLE, wrong, expected);
    }

    /**
     * @return the expected dimension.
     */
    public int getDimension() {
        return dimension;
    }
}