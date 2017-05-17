import sys


class Element:
    """An element of a poset"""
    def __init__(self, elem_id, preds):
        self.elem_id = elem_id
        self.preds = preds
        self.chain_next = None
        self.chain = None

    def __repr__(self):
        return self.elem_id

    def __str__(self):
        return self.elem_id

    def before(self, other):
        """Returns True if the current element is before other in the poset."""
        return self in other.all_preds()

    def all_preds(self):
        """Returns a generator over all (including transitive) predecessors."""
        visited = set()
        queue = self.preds.copy()
        while queue:
            elem = queue[0]
            queue = queue[1:]
            if elem in visited:
                continue
            visited.add(elem)
            queue += elem.preds
            yield elem


class Chain:
    """A chain data structure"""
    def __init__(self, head=None, tail=None):
        self.head = head
        self.tail = tail
        current = head
        while current is not None:
            current.chain = self
            current = current.chain_next

    def append(self, elem):
        if self.tail is None:
            self.head = elem
        else:
            self.tail.chain_next = elem
        self.tail = elem
        elem.chain = self


class ChainDecomposition:
    """A poset decomposed as a union of chains"""
    def __init__(self):
        self.chains = set()

    def add_elem(self, elem):
        for chain in self.chains:
            if chain.tail.before(elem):
                chain.append(elem)
                return
        self.chains.add(Chain(elem, elem))

    def rewire(self, a, b):
        # Rewire the elements
        a.chain_next = b

        # Rewire the chains
        a.chain.tail = b.chain.tail
        current = b
        while current is not None:
            current.chain = a.chain
            current = current.chain_next

    def reduce(self, a, b, pairs):
        while True:
            b_chain = b.chain
            self.rewire(a, b)
            if b not in pairs:
                break
            a, b = pairs[b]
        self.chains.discard(b_chain)

    def merge(self):
        current_q = [chain.head for chain in self.chains]
        next_q = []
        pairs = {}
        while current_q:
            for b in current_q:
                for a in b.all_preds():
                    a_next = a.chain_next
                    if a_next is None:
                        # We have found a reducing sequence starting with a
                        self.reduce(a, b, pairs)
                        return True
                    if a_next in pairs:
                        continue
                    pairs[a_next] = (a, b)
                    next_q.append(a_next)
            current_q = next_q
            next_q = []
        return False

    def print_decomposition(self):
        for chain in self.chains:
            elem = chain.head
            while elem is not None:
                print(elem.elem_id, end=' ')
                elem = elem.chain_next
            print()


class PosetReader:
    """Reads and constructs a poset from stdin"""
    def __init__(self):
        self.elems = {}
        self.chain_decomposition = ChainDecomposition()

    def read(self):
        for line in sys.stdin:
            elem = self.new_elem(line)
            self.chain_decomposition.add_elem(elem)
        while self.chain_decomposition.merge():
            pass

    def new_elem(self, line):
        ids = line.split()
        preds = [self.elems[elem_id] for elem_id in ids[1:]]
        elem = Element(ids[0], preds)
        self.elems[elem.elem_id] = elem
        return elem

    def print_decomposition(self):
        self.chain_decomposition.print_decomposition()


if __name__ == '__main__':
    reader = PosetReader()
    reader.read()
    reader.print_decomposition()

