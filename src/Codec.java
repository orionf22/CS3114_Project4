/**
 * The {@code Codec} interface should be implemented by a subclass that needs to
 * translate/ encrypt/decrypt data from one format to bytes in preparation for
 * memory storage. It is provided so that the {@link Controller} class can make
 * use of any properly implemented {@code Codec} to execute such functions, as
 * opposed to hard-coding the methods.
 * <p/>
 * @author rinaldi1
 * @author orionf22
 */
public interface Codec<E>
{

	/**
	 * Decodes {@code bytes} into an {@code E} object.
	 * <p/>
	 * @param bytes the bytes to decode
	 * <p/>
	 * @return the decoded bytes as {@code E}
	 */
	public E decode(byte[] bytes);

	/**
	 * Encodes {@code stuff} as a byte array.
	 * <p/>
	 * @param stuff the object to encode
	 * <p/>
	 * @return the encoded object as a byte array
	 */
	public byte[] encode(E stuff);
}
