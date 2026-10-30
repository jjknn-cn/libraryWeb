/* 公共脚本：登录态、请求封装、导航壳、提示 */
const App = {
    user: null,
    menus: [
        { key: 'index', text: '首页', href: 'index.html', roles: ['管理员', '操作员', '学生', '教师'] },
        { key: 'borrow', text: '借书', href: 'borrow.html', roles: ['管理员', '操作员', '学生', '教师'] },
        { key: 'return', text: '还书', href: 'return.html', roles: ['管理员', '操作员', '学生', '教师'] },
        { key: 'my-loans', text: '个人借阅查询', href: 'my-loans.html', roles: ['管理员', '操作员', '学生', '教师'] },
        { key: 'books', text: '图书管理', href: 'books.html', roles: ['管理员', '操作员'] },
        { key: 'users', text: '用户信息查询', href: 'users.html', roles: ['管理员'] },
        { key: 'loan-query', text: '借阅记录查询', href: 'loan-query.html', roles: ['管理员'] },
        { key: 'stats', text: '借阅数据统计', href: 'stats.html', roles: ['管理员'] },
        { key: 'recommend', text: '推荐图书', href: 'recommend.html', roles: ['教师'] },
        { key: 'review', text: '审阅推荐', href: 'review.html', roles: ['管理员'] }
    ],

    /** 校验登录态；roles 不为空时校验角色 */
    async boot(roles) {
        try {
            const resp = await axios.get('api/auth/current');
            this.user = resp.data.data;
        } catch (e) {
            if (!e.response || e.response.status === 401 || e.response.status === 403) {
                location.href = 'login.html';
            } else {
                this.toast('无法连接服务器，请确认 Tomcat 已启动', 'error');
            }
            return false;
        }
        if (roles && roles.length && !roles.includes(this.user.role)) {
            location.href = 'index.html';
            return false;
        }
        return true;
    },

    async request(method, url, data, params) {
        try {
            const config = { method: method, url: url };
            if (data !== undefined) config.data = data;
            if (params) config.params = params;
            const resp = await axios(config);
            return resp.data.data;
        } catch (e) {
            if (e.response && e.response.status === 401) {
                location.href = 'login.html';
                throw new Error('登录已过期');
            }
            let msg = '请求失败，请稍后重试';
            if (e.response && e.response.data && e.response.data.msg) msg = e.response.data.msg;
            else if (!e.response) msg = '网络异常，请确认服务已启动';
            throw new Error(msg);
        }
    },

    get(url, params) { return this.request('get', url, undefined, params); },
    post(url, data) { return this.request('post', url, data); },

    fmt(value) {
        if (value === null || value === undefined || value === '') return '—';
        return String(value).replace('T', ' ').slice(0, 19);
    },

    day(value) {
        if (!value) return '—';
        return String(value).slice(0, 10);
    },

    shortId(value) {
        if (!value) return '—';
        return String(value).slice(0, 8);
    },

    statusClass(status) {
        if (status === '已还') return 'badge-green';
        if (status === '超期') return 'badge-red';
        if (status === '未还') return 'badge-amber';
        if (status === '正常') return 'badge-green';
        if (status === '已废弃') return 'badge-gray';
        if (status === '待审核') return 'badge-amber';
        if (status === '已通过') return 'badge-green';
        if (status === '已驳回') return 'badge-red';
        return 'badge-gray';
    },

    toast(message, type) {
        let box = document.getElementById('toast-box');
        if (!box) {
            box = document.createElement('div');
            box.id = 'toast-box';
            document.body.appendChild(box);
        }
        const el = $('<div class="toast"></div>').addClass(type || '').text(message);
        $(box).append(el);
        setTimeout(function () { el.fadeOut(250, function () { el.remove(); }); }, 2600);
    },

    async logout() {
        try { await axios.post('api/auth/logout'); } catch (e) { /* 忽略 */ }
        location.href = 'login.html';
    }
};

/* 页面统一外壳：顶栏 + 菜单 + 标题 */
const AppShell = {
    props: { title: String, active: String },
    template: `
    <div>
        <header class="topbar">
            <div class="brand">📚 图书馆管理系统</div>
            <nav class="topnav">
                <a v-for="m in menus" :key="m.key" :href="m.href" :class="{ active: m.key === active }">{{ m.text }}</a>
            </nav>
            <div class="userbox">
                <span class="avatar">{{ user.name ? user.name.slice(0, 1) : '?' }}</span>
                <span>{{ user.name }}</span>
                <span class="badge badge-blue">{{ user.role }}</span>
                <button class="btn btn-sm btn-outline" @click="logout">退出</button>
            </div>
        </header>
        <div class="page">
            <div class="page-head">
                <div>
                    <h1>{{ title }}</h1>
                    <div class="sub" v-if="sub">{{ sub }}</div>
                </div>
                <slot name="head"></slot>
            </div>
            <slot></slot>
        </div>
    </div>`,
    data() { return { user: App.user, sub: '' }; },
    computed: {
        menus() {
            const role = App.user.role;
            return App.menus.filter(function (m) { return m.roles.includes(role); });
        }
    },
    methods: { logout() { App.logout(); } }
};
