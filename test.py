import copy

class Production:
    def __init__(self, left, right):
        self.left = left
        self.right = right

    def __eq__(self, other):
        return self.left == other.left and self.right == other.right

    def __repr__(self):
        return f"{self.left} -> {' '.join(self.right)}"

class Item:
    def __init__(self, left, right, dot_pos):
        self.left = left
        self.right = right
        self.dot_pos = dot_pos

    def __eq__(self, other):
        return self.left == other.left and self.right == other.right and self.dot_pos == other.dot_pos

    def __hash__(self):
        return hash((self.left, tuple(self.right), self.dot_pos))

    def __repr__(self):
        return f"{self.left} -> {' '.join(self.right[:self.dot_pos] + ['.'] + self.right[self.dot_pos:])}"

def closure(I, productions):
    J = copy.deepcopy(I)
    while True:
        new_items = []
        for item in J:
            if item.dot_pos < len(item.right):
                symbol = item.right[item.dot_pos]
                if symbol in nonterm_userdef:
                    for prod in productions:
                        if prod.left == symbol:
                            new_item = Item(prod.left, prod.right, 0)
                            if new_item not in J:
                                new_items.append(new_item)
        if not new_items:
            break
        J.extend(new_items)
    return J

def goto(I, X, productions):
    goto_set = []
    for item in I:
        if item.dot_pos < len(item.right) and item.right[item.dot_pos] == X:
            new_item = copy.deepcopy(item)
            new_item.dot_pos += 1
            goto_set.append(new_item)
    return closure(goto_set, productions)

def canonicalCollection(productions, start_symbol):
    C = [closure([Item(productions[0].left, productions[0].right, 0)], productions)]
    while True:
        new_items = []
        for I in C:
            for X in term_userdef + nonterm_userdef:
                goto_I = goto(I, X, productions)
                if goto_I and goto_I not in C:
                    new_items.append(goto_I)
        if not new_items:
            break
        C.extend(new_items)
    return C

def compute_first(symbol, productions):
    first = set()
    if symbol in term_userdef or symbol == 'ε':
        first.add(symbol)
    elif symbol in nonterm_userdef:
        for prod in productions:
            if prod.left == symbol:
                if prod.right[0] == 'ε':
                    first.add('ε')
                else:
                    first_of_right = compute_first(prod.right[0], productions)
                    first.update(first_of_right - {'ε'})
                    if 'ε' in first_of_right and len(prod.right) > 1:
                        first.update(compute_first(prod.right[1], productions))
    return first

def compute_follow(productions, start_symbol):
    follow = {nt: set() for nt in nonterm_userdef}
    follow[start_symbol].add('$')
    
    while True:
        updated = False
        for prod in productions:
            for i, symbol in enumerate(prod.right):
                if symbol in nonterm_userdef:
                    if i < len(prod.right) - 1:
                        first_next = compute_first(prod.right[i+1], productions)
                        follow_update = first_next - {'ε'}
                        if follow[symbol] != follow[symbol].union(follow_update):
                            follow[symbol].update(follow_update)
                            updated = True
                        if 'ε' in first_next:
                            if follow[symbol] != follow[symbol].union(follow[prod.left]):
                                follow[symbol].update(follow[prod.left])
                                updated = True
                    else:
                        if follow[symbol] != follow[symbol].union(follow[prod.left]):
                            follow[symbol].update(follow[prod.left])
                            updated = True
        if not updated:
            break
    
    return follow

def construct_slr_table(C, productions, start_symbol):
    action = {}
    goto_table = {}
    follow = compute_follow(productions, start_symbol)
    
    for i, I in enumerate(C):
        for item in I:
            if item.dot_pos < len(item.right):
                symbol = item.right[item.dot_pos]
                if symbol in term_userdef:
                    j = C.index(goto(I, symbol, productions))
                    action[(i, symbol)] = f's{j}'
            else:
                if item.left == start_symbol and item.right == productions[0].right:
                    action[(i, '$')] = 'acc'
                else:
                    for a in follow[item.left]:
                        prod_index = next(idx for idx, p in enumerate(productions) if p.left == item.left and p.right == item.right)
                        action[(i, a)] = f'r{prod_index}'
        
        for A in nonterm_userdef:
            goto_I = goto(I, A, productions)
            if goto_I:
                j = C.index(goto_I)
                goto_table[(i, A)] = j
    
    return action, goto_table

def generate_md_table(action, goto_table, term_userdef, nonterm_userdef):
    headers = ['State'] + term_userdef + ['$'] + nonterm_userdef
    table = [headers]
    
    max_states = max(max(state for state, _ in action.keys()), max(state for state, _ in goto_table.keys()))
    
    for state in range(max_states + 1):
        row = [str(state)]
        for symbol in term_userdef + ['$'] + nonterm_userdef:
            if (state, symbol) in action:
                row.append(action[(state, symbol)])
            elif (state, symbol) in goto_table:
                row.append(str(goto_table[(state, symbol)]))
            else:
                row.append('')
        table.append(row)
    
    md_table = '| ' + ' | '.join(headers) + ' |\n'
    md_table += '|' + '---|' * len(headers) + '\n'
    for row in table[1:]:
        md_table += '| ' + ' | '.join(row) + ' |\n'
    
    return md_table

def main():
    # Define the grammar
    rules = [
        "S -> T",
        "T -> R",
        "T -> a T c",
        "R -> ε",
        "R -> b R"
    ]
    
    # Define non-terminals, terminals, and start symbol
    global nonterm_userdef, term_userdef, start_symbol
    nonterm_userdef = ["S", 'T', "R"]
    term_userdef = ['a', 'c', 'b']
    start_symbol = nonterm_userdef[0]
    
    # Create Production objects
    productions = [Production(rule.split('->')[0].strip(), rule.split('->')[1].strip().split()) for rule in rules]
    
    # Generate the canonical collection
    C = canonicalCollection(productions, start_symbol)
    
    # Construct the SLR parsing table
    action, goto_table = construct_slr_table(C, productions, start_symbol)
    
    # Generate the Markdown table
    md_table = generate_md_table(action, goto_table, term_userdef, nonterm_userdef)
    
    # Write the table to a file
    with open('slr_parsing_table.md', 'w') as f:
        f.write(md_table)

    print("SLR parsing table has been generated and saved to 'slr_parsing_table.md'")

if __name__ == "__main__":
    main()