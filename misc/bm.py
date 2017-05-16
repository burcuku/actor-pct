import sys


class Element:
    """An element of a poset"""
    def __init__(self, elem_id, preds):
        self.elem_id = elem_id
        self.preds = preds
        self.succs = []
        self.chain_next = None
        self.chain_prev = None
        self.chain = None

    def __repr__(self):
        return self.elem_id

    def __str__(self):
        return self.elem_id

    def add_succ(self, elem):
        self.succs.append(elem)

    def before(self, other):
        return other in self.all_after()

    def all_after(self):
        visited = set()
        queue = self.succs.copy()
        while queue:
            elem = queue[0]
            queue = queue[1:]
            if elem in visited:
                continue
            visited.add(elem)
            queue += elem.succs
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
            elem.chain_prev = self.tail
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
        bb = a.chain_next
        a.chain_next = b
        b.chain_prev = a

        # Rewire the chains
        chain_a = a.chain
        chain_b = b.chain
        if bb is None:
            chain_a.tail = chain_b.tail
            chain_b.head = chain_b.tail = None
        else:
            chain_b.head = bb
            t = chain_b.tail
            chain_b.tail = chain_a.tail
            chain_a.tail = t

            # No need to set chain for other nodes in the new chain_b
            bb.chain = chain_b
        current = b
        while current is not None:
            current.chain = chain_a
            current = current.chain_next
        return bb

    def reduce(self, b, prev):
        self.chains.discard(b.chain)
        while b is not None:
            a = prev[b]
            b = self.rewire(a, b)

    def merge(self):
        current_q = [chain.tail for chain in self.chains]
        next_q = []
        prev = {}
        while current_q:
            for a in current_q:
                for b in a.all_after():
                    if b in prev:
                        continue
                    prev[b] = a
                    if b.chain_prev is None:
                        # We have found a reducing sequence
                        self.reduce(b, prev)
                        return True
                    else:
                        next_q.append(b.chain_prev)
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
        for pred in preds:
            pred.add_succ(elem)
        self.elems[ids[0]] = elem
        return elem

    def print_decomposition(self):
        self.chain_decomposition.print_decomposition()


if __name__ == '__main__':
    reader = PosetReader()
    reader.read()
    reader.print_decomposition()

