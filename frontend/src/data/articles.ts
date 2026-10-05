export type ArticleSection = {
  heading: string
  paragraphs: string[]
  bullets?: string[]
  code?: string
}

export type Article = {
  id: number
  title: string
  summary: string
  category: string
  tags: string[]
  preview: string[]
  sections: ArticleSection[]
  featured?: boolean
  wide?: boolean
}

export const articles: Article[] = [
  {
    id: 1,
    title: '最短路算法：Dijkstra 与堆优化详解',
    summary: '从非负权图上的松弛操作出发，实现堆优化 Dijkstra，并说明复杂度和常见错误。',
    category: '题解', tags: ['图论', '最短路', 'C++', '题解'], featured: true,
    preview: ['using State = pair<long long, int>;', 'priority_queue<State, vector<State>,', '  greater<State>> pq;', 'dist[source] = 0;', 'pq.push({0, source});'],
    sections: [
      { heading: '适用条件与核心思路', paragraphs: ['Dijkstra 适用于边权非负的图。从起点距离为 0 开始，每次取出当前距离最小的顶点，再尝试用它更新相邻顶点的距离，这一步称为松弛。', '优先队列中可能保留同一顶点的旧距离。取出状态时若距离与最新的 dist 不同，直接跳过。负权边会破坏“取出最小值即可确定最短路”的前提，不能直接使用该算法。'] },
      { heading: 'C++ 实现', paragraphs: ['图的顶点编号为 0 到 n−1。邻接表中的 pair 分别表示终点和非负边权；不可达点的距离保持为 INF。'], code: `#include <climits>
#include <functional>
#include <queue>
#include <utility>
#include <vector>
using namespace std;

vector<long long> dijkstra(
    const vector<vector<pair<int, int>>>& graph, int source) {
    const long long INF = LLONG_MAX / 4;
    vector<long long> dist(graph.size(), INF);
    using State = pair<long long, int>; // 距离、顶点
    priority_queue<State, vector<State>, greater<State>> pq;
    dist[source] = 0;
    pq.push({0, source});

    while (!pq.empty()) {
        auto [distance, u] = pq.top();
        pq.pop();
        if (distance != dist[u]) continue;
        for (auto [v, weight] : graph[u]) {
            if (dist[v] > distance + weight) {
                dist[v] = distance + weight;
                pq.push({dist[v], v});
            }
        }
    }
    return dist;
}` },
      { heading: '验证与复杂度', paragraphs: ['例如有边 0→1（权重 2）、0→2（权重 5）、1→2（权重 1），从 0 出发得到距离 0、2、3。最初到 2 的距离 5 会被更新为 3，队列里的旧状态 5 会被跳过。', '使用邻接表和二叉堆时，时间复杂度通常写作 O((V+E) log V)，空间复杂度为 O(V+E)。距离可能累加超过 int 范围，因此使用 long long。'] },
    ],
  },
  {
    id: 2,
    title: '并查集（Disjoint Set Union）模板详解',
    summary: '用路径压缩与按大小合并维护连通分量，快速判断两点是否属于同一集合。',
    category: '算法模板', tags: ['数据结构', '并查集', '模板', 'C++'],
    preview: ['int find(int x) {', '  if (parent[x] != x)', '    parent[x] = find(parent[x]);', '  return parent[x];', '}'],
    sections: [
      { heading: '什么时候使用', paragraphs: ['并查集维护一组互不相交的集合，支持查找元素所在集合的代表，以及合并两个集合。它适合动态增加连接、询问两点是否连通，例如无向图连通性和 Kruskal 最小生成树。', '普通并查集不能直接删除边，也不能回答两点间的具体路径。'] },
      { heading: '路径压缩与按大小合并', paragraphs: ['find 在返回代表元素时把沿途节点直接连接到根，缩短后续查询路径。unite 把较小的树接到较大的树上，避免树过深。'], code: `#include <algorithm>
#include <numeric>
#include <vector>
using namespace std;

struct DSU {
    vector<int> parent, size;
    explicit DSU(int n) : parent(n), size(n, 1) {
        iota(parent.begin(), parent.end(), 0);
    }
    int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }
    bool unite(int a, int b) {
        a = find(a);
        b = find(b);
        if (a == b) return false;
        if (size[a] < size[b]) swap(a, b);
        parent[b] = a;
        size[a] += size[b];
        return true;
    }
    bool same(int a, int b) { return find(a) == find(b); }
};` },
      { heading: '例子与复杂度', paragraphs: ['对编号 0、1、2、3，先合并 0 与 1，再合并 1 与 2，则 same(0, 2) 为 true，same(0, 3) 为 false。', '两种优化同时使用时，单次操作的均摊时间复杂度为 O(α(n))，α 是增长极慢的反阿克曼函数；空间复杂度为 O(n)。'] },
    ],
  },
  {
    id: 3,
    title: '背包问题全解析：01 / 完全 / 多重背包',
    summary: '分清物品可选次数，用一维 DP 的循环方向处理 01 背包与完全背包。',
    category: '题解', tags: ['动态规划', '背包问题', '题解', 'C++'],
    preview: ['for (int i = 0; i < n; ++i)', '  for (int c = capacity; c >= weight[i]; --c)', '    dp[c] = max(dp[c],', '      dp[c - weight[i]] + value[i]);'],
    sections: [
      { heading: '状态与转移', paragraphs: ['设 dp[c] 为容量不超过 c 时能获得的最大价值。处理重量 w、价值 v 的物品时，可以不选，也可以在剩余容量 c−w 的最优方案上加上 v，所以转移是 dp[c] = max(dp[c], dp[c−w] + v)。这里假设重量为正，空背包价值为 0。'] },
      { heading: '01 背包：每件最多一次', paragraphs: ['容量必须从大到小遍历。这样读取 dp[c−w] 时，该位置仍是上一件物品处理完后的结果，同一件物品不会被重复选取。'], code: `#include <algorithm>
#include <vector>
using namespace std;

vector<long long> zeroOneKnapsack(
    const vector<int>& weight, const vector<int>& value,
    int capacity) {
    vector<long long> dp(capacity + 1, 0);
    for (size_t i = 0; i < weight.size(); ++i) {
        for (int c = capacity; c >= weight[i]; --c) {
            dp[c] = max(dp[c], dp[c - weight[i]] + value[i]);
        }
    }
    return dp;
}` },
      { heading: '完全背包与多重背包', paragraphs: ['完全背包允许一件物品使用任意次，因此容量从小到大遍历，让当前物品本轮更新后的 dp[c−w] 继续参与转移。只需把上面代码的内层循环改为 for (int c = weight[i]; c <= capacity; ++c)。', '多重背包给每种物品一个数量上限。数据量小时可逐件转成 01 背包；数量较大时可用二进制拆分把数量拆成 1、2、4……及剩余数量的若干组，再按 01 背包处理。'] },
      { heading: '手算检查', paragraphs: ['容量为 4，只有一件重量 2、价值 3 的物品：01 背包答案为 3；完全背包可选两次，答案为 6。若两种写法得到相同结果，通常要检查容量循环方向。', '一维数组的空间复杂度为 O(C)。01 背包和完全背包的时间复杂度均为 O(nC)。'] },
    ],
  },
  {
    id: 4,
    title: '树状数组（Fenwick）详解与模板',
    summary: '利用 lowbit 维护前缀和，支持单点加值与区间求和。',
    category: '算法模板', tags: ['数据结构', '树状数组', '模板', 'C++'],
    preview: ['int lowbit(int x) { return x & -x; }', 'void add(int x, long long delta) {', '  for (; x <= n; x += lowbit(x))', '    tree[x] += delta;', '}'],
    sections: [
      { heading: '为什么要用 1 起始下标', paragraphs: ['树状数组的 tree[x] 保存一个以 x 结尾、长度为 lowbit(x) 的区间和。例如 tree[6] 覆盖位置 5 到 6，因为 lowbit(6)=2。下标必须从 1 开始，否则 lowbit(0)=0 会使更新循环无法前进。'] },
      { heading: '单点更新与前缀查询', paragraphs: ['更新位置 x 时不断跳到 x+lowbit(x)，让所有包含该位置的节点加上变化量；查询前缀时不断减去 lowbit(x)，把不重叠区间的和相加。'], code: `#include <vector>
using namespace std;

struct Fenwick {
    int n;
    vector<long long> tree;
    explicit Fenwick(int n) : n(n), tree(n + 1, 0) {}
    static int lowbit(int x) { return x & -x; }
    void add(int x, long long delta) {
        for (; x <= n; x += lowbit(x)) tree[x] += delta;
    }
    long long prefix(int x) const {
        long long result = 0;
        for (; x > 0; x -= lowbit(x)) result += tree[x];
        return result;
    }
    long long range(int left, int right) const {
        return prefix(right) - prefix(left - 1);
    }
};` },
      { heading: '例子与边界', paragraphs: ['把数组 [2, 1, 3, 4] 依次用 add(i, a[i−1]) 插入后，range(2, 4)=1+3+4=8。若第三个数增加 5，调用 add(3, 5)，同一区间的和变成 13。', '每次更新和查询都是 O(log n)，额外空间为 O(n)。这个模板处理“单点加值、区间求和”；区间加值等变体需要额外技巧。'] },
    ],
  },
  {
    id: 5,
    title: 'KMP 字符串匹配模板与例题',
    summary: '用前缀函数记录可复用的匹配长度，在线性时间内找出全部匹配位置。',
    category: '算法模板', tags: ['字符串', 'KMP', '模板', 'C++'],
    preview: ['for (int i = 1, j = 0; i < m; ++i) {', '  while (j && pattern[i] != pattern[j])', '    j = pi[j - 1];', '  if (pattern[i] == pattern[j]) ++j;', '  pi[i] = j;', '}'],
    sections: [
      { heading: '前缀函数的含义', paragraphs: ['pi[i] 表示模式串前 i+1 个字符中，相等的真前缀和真后缀的最大长度。失配时不必把文本指针倒退，而是让已匹配长度回退到 pi[j−1]，保留仍可能有效的前缀。'] },
      { heading: '查找所有匹配', paragraphs: ['下列实现要求模式串非空，返回所有从 0 开始的匹配位置，包括重叠匹配。'], code: `#include <string>
#include <vector>
using namespace std;

vector<int> kmpFindAll(const string& text,
                       const string& pattern) {
    if (pattern.empty()) return {};
    int m = static_cast<int>(pattern.size());
    vector<int> pi(m, 0), positions;
    for (int i = 1, j = 0; i < m; ++i) {
        while (j > 0 && pattern[i] != pattern[j]) j = pi[j - 1];
        if (pattern[i] == pattern[j]) ++j;
        pi[i] = j;
    }
    for (int i = 0, j = 0; i < static_cast<int>(text.size()); ++i) {
        while (j > 0 && text[i] != pattern[j]) j = pi[j - 1];
        if (text[i] == pattern[j]) ++j;
        if (j == m) {
            positions.push_back(i - m + 1);
            j = pi[j - 1];
        }
    }
    return positions;
}` },
      { heading: '例子与复杂度', paragraphs: ['文本 ababa、模式 aba 的匹配起点是 0 和 2。找到第一个匹配后继续回退到 pi[2]=1，才能发现重叠的第二个匹配。', '构建前缀函数需要 O(m)，扫描文本需要 O(n)，总时间 O(n+m)，额外空间 O(m)（不含输出结果）。'] },
    ],
  },
  {
    id: 6,
    title: 'ACM 竞赛复盘方法：从读题到赛后整理',
    summary: '一份可直接使用的赛后复盘清单：记录决策、定位错误，并把未完成题目转化为练习。',
    category: '竞赛经验', tags: ['竞赛经验', 'ACM', '比赛总结', '成长'], wide: true,
    preview: ['读题与分工', '提交与反馈', '赛后复现', '知识点归档'],
    sections: [
      { heading: '先记录过程，再评价结果', paragraphs: ['复盘不是只看最终排名。先按时间顺序写下每道题何时读完、何时确定做法、何时首次提交，以及 AC、WA、TLE 等反馈。这样能区分知识欠缺、实现错误和时间分配问题。', '团队赛还应记录分工与沟通节点：谁负责推导，谁负责实现，什么时候需要交换思路。这里提供的是通用方法，不指代任何真实比赛经历。'] },
      { heading: '把未通过的题目重新做一遍', paragraphs: ['在不看题解的情况下复现失败提交，用最小反例确认问题。若是 WA，优先检查边界和数据类型；若是 TLE，估算实际输入规模与算法复杂度；若是 RE，检查数组越界、除零和递归深度。', '确认原因后再阅读标准做法，并独立重写一次。只保存一份正确代码，往往难以记住当时为什么卡住。'] },
      { heading: '一份可复用的复盘记录', paragraphs: ['每道题可以按下面的格式记录。下次遇到相同模式时，重点回看“错误原因”和“识别信号”。'], bullets: ['题目与关键约束：输入规模、时间限制、特殊边界。', '最初思路与复杂度：为什么选择这个算法？', '提交反馈与最小反例：错误由哪一组数据触发？', '正确做法与识别信号：什么条件提示应该使用这个方法？', '复习计划：隔几天不看答案重新完成一次。'] },
    ],
  },
  {
    id: 7,
    title: '二分查找的边界：从模板到应用',
    summary: '用左闭右开的区间寻找第一个不小于目标值的位置，避免边界和死循环。',
    category: '算法模板', tags: ['搜索', '模板', 'C++'],
    preview: ['while (left < right) {', '  int mid = left + (right - left) / 2;', '  if (a[mid] >= target) right = mid;', '  else left = mid + 1;', '}'],
    sections: [
      { heading: '明确区间含义', paragraphs: ['假设数组已经按非递减顺序排列。要找第一个满足 a[i] >= target 的位置，可维护候选区间 [left, right)，初始为 [0, n)。当区间为空时，left 就是答案；若所有元素都小于目标值，答案为 n。'] },
      { heading: '左边界模板', paragraphs: ['mid 属于区间内。当 a[mid] 满足条件时，mid 可能是答案，保留它并令 right=mid；否则丢弃 [left, mid]，令 left=mid+1。'], code: `#include <vector>
using namespace std;

int lowerBoundIndex(const vector<int>& a, int target) {
    int left = 0, right = static_cast<int>(a.size());
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (a[mid] >= target) right = mid;
        else left = mid + 1;
    }
    return left;
}` },
      { heading: '例子与复杂度', paragraphs: ['在 [1, 2, 2, 4] 中搜索 2，返回 1；搜索 3，返回 3；搜索 5，返回 4。若要确认目标值确实存在，还需检查返回位置小于数组长度且 a[position] == target。', '每次循环至少缩小一半区间，因此时间复杂度为 O(log n)，额外空间为 O(1)。不变量和循环边界要一起确定，不能只替换某一行比较符号。'] },
    ],
  },
  {
    id: 8,
    title: '408 笔记：数据结构核心知识点',
    summary: '以线性结构、树、图和排序为线索，整理复习时容易混淆的性质与复杂度。',
    category: '408笔记', tags: ['408笔记', '数据结构', '复习'],
    preview: ['线性表 · 栈与队列', '树与二叉树 · 图', '查找 · 排序', '时间复杂度 · 空间复杂度'],
    sections: [
      { heading: '线性结构：存储方式决定代价', paragraphs: ['顺序表支持 O(1) 随机访问，但在中间插入或删除通常需要移动元素，代价为 O(n)。链表可在已知节点位置时用 O(1) 完成局部插入或删除，但查找第 i 个元素仍需 O(n)。', '栈遵循后进先出，队列遵循先进先出。用数组实现循环队列时，要明确队头、队尾指针的含义以及判空、判满规则。'] },
      { heading: '树与图：先确认遍历目标', paragraphs: ['二叉树的前序、中序、后序遍历差别在于访问根节点的时机。二叉搜索树的中序遍历会得到非递减序列，但只有树保持平衡时，查找的最坏时间才可维持在 O(log n) 量级。', '图的 BFS 借助队列按边数分层，可求无权图的最短路径；DFS 常用于连通性、环检测及回溯。遍历复杂度在邻接表存储下是 O(V+E)。'] },
      { heading: '排序：别只记平均时间', paragraphs: ['归并排序时间复杂度为 O(n log n)，常见数组实现需要 O(n) 额外空间，且可以保持稳定性。快速排序平均 O(n log n)，但极端划分下会退化到 O(n²)；常见原地实现不稳定。', '复习复杂度时，还要分清“额外空间”是否计入递归栈、算法是否稳定，以及题目数据是否存在大量重复值。'] },
    ],
  },
]

export function articleUrl(id: number, fromQuery = ''): string {
  const params = new URLSearchParams({ id: String(id) })
  if (fromQuery) params.set('from', fromQuery)
  return `./article.html?${params}`
}
