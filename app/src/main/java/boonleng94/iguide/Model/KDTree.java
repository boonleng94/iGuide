package boonleng94.iguide.Model;

//The KDTree implementation is not actually needed since Navigator does the same job of returning nearest neighbour
//KD Tree to find nearest neighbour
public class KDTree {
    KDNode Root;
    double d_min;
    double x_min[], x_max[];
    KDNode nearest_neighbour;
    KDNode CheckedNodes[];
    KDNode List[];
    int KD_id;
    int nList;
    int checked_nodes;
    int n_boundary;
    boolean max_boundary[], min_boundary[];

    public KDTree(int i) {
        Root = null;
        KD_id = 1;
        nList = 0;
        List = new KDNode[i];
        CheckedNodes = new KDNode[i];
        max_boundary = new boolean[2];
        min_boundary = new boolean[2];
        x_min = new double[2];
        x_max = new double[2];
    }

    public boolean add(double[] x) {
        if (nList >= 2000000 - 1)
            return false;

        if (Root == null) {
            Root = new KDNode(x, 0);
            Root.id = KD_id++;
            List[nList++] = Root;
        } else {
            KDNode pNode;
            if ((pNode = Root.Insert(x)) != null) {
                pNode.id = KD_id++;
                List[nList++] = pNode;
            }
        }
        return true;
    }

    public KDNode find_nearest(double[] x) {
        if (Root == null)
            return null;
        checked_nodes = 0;
        KDNode parent = Root.FindParent(x);
        nearest_neighbour = parent;
        d_min = Root.distance2(x, parent.x, 2);

        if (parent.equal(x, parent.x, 2) == true)
            return nearest_neighbour;

        search_parent(parent, x);
        uncheck();
        return nearest_neighbour;
    }

    public void check_subtree(KDNode node, double[] x) {
        if ((node == null) || node.checked)
            return;

        CheckedNodes[checked_nodes++] = node;
        node.checked = true;
        set_bounding_cube(node, x);

        int dim = node.axis;
        double d = node.x[dim] - x[dim];

        if (d * d > d_min) {
            if (node.x[dim] > x[dim])
                check_subtree(node.Left, x);
            else
                check_subtree(node.Right, x);
        } else {
            check_subtree(node.Left, x);
            check_subtree(node.Right, x);
        }
    }

    public void set_bounding_cube(KDNode node, double[] x) {
        if (node == null)
            return;
        int d = 0;
        double dx;
        for (int k = 0; k < 2; k++) {
            dx = node.x[k] - x[k];
            if (dx > 0) {
                dx *= dx;
                if (!max_boundary[k]) {
                    if (dx > x_max[k])
                        x_max[k] = dx;
                    if (x_max[k] > d_min) {
                        max_boundary[k] = true;
                        n_boundary++;
                    }
                }
            } else {
                dx *= dx;
                if (!min_boundary[k]) {
                    if (dx > x_min[k])
                        x_min[k] = dx;
                    if (x_min[k] > d_min) {
                        min_boundary[k] = true;
                        n_boundary++;
                    }
                }
            }
            d += dx;
            if (d > d_min)
                return;
        }

        if (d < d_min) {
            d_min = d;
            nearest_neighbour = node;
        }
    }

    public KDNode search_parent(KDNode parent, double[] x) {
        for (int k = 0; k < 2; k++) {
            x_min[k] = x_max[k] = 0;
            max_boundary[k] = min_boundary[k] = false; //
        }
        n_boundary = 0;

        KDNode search_root = parent;
        while (parent != null && (n_boundary != 2 * 2)) {
            check_subtree(parent, x);
            search_root = parent;
            parent = parent.Parent;
        }
        return search_root;
    }

    public void uncheck() {
        for (int n = 0; n < checked_nodes; n++)
            CheckedNodes[n].checked = false;
    }
}
