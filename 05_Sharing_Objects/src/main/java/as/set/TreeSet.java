package as.set;

public class TreeSet<E extends Comparable<E>> implements ITreeSet<E> {

    private final E root;
    private final ITreeSet<E> leftTree;
    private final ITreeSet<E> rightTree;

    public TreeSet() {
        root = null;
        leftTree = null;
        rightTree = null;
    }

    public TreeSet(E root, ITreeSet<E> leftTree, ITreeSet<E> rightTree) {
        this.root = root;
        this.leftTree = leftTree;
        this.rightTree = rightTree;
    }

    public static <E extends Comparable<E>> ITreeSet<E> empty() {
        return new TreeSet<>();
    }

    public E getRoot() {
        return root;
    }

    @Override
    public ITreeSet<E> insert(E e) {
        boolean contains = contains(e);
        if (contains) {
            return this;
        } else {
            if (isEmpty()) {
                return new TreeSet<>(e, empty(), empty());
            }
            int result = root.compareTo(e);
            if (result < 0) {
                return new TreeSet<>(e, empty(), this);
            } else {
                return new TreeSet<>(e, this, empty());
            }
        }

    }

    @Override
    public boolean contains(E e) {
        if (isEmpty())
            return false;
        int result = root.compareTo(e);
        if (result == 0) {
            return true;
        } else if (result < 0) {
            return leftTree.contains(e);
        } else {
            return rightTree.contains(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }
}
