import { useEffect, useMemo, useState, type FormEvent } from "react";
import {
  ArrowDownRight,
  ArrowLeft,
  ArrowRight,
  Bell,
  Check,
  ChevronDown,
  CircleHelp,
  Club,
  Heart,
  LogIn,
  MessageCircle,
  Plus,
  Search,
  Send,
  Shield,
  Sparkles,
  Users,
  X,
} from "lucide-react";
import { api, ApiError, type Comment, type Community, type Post, type User } from "./api";

type FeedPost = Post & { comments: Comment[] };

const initialCommunities = [
  { name: "Arsenal Women", slug: "arsenal-women", description: "For the Arsenal Women's team and everyone cheering them on.", icon: "🔴" },
  { name: "Chelsea Women", slug: "chelsea-women", description: "The home of Chelsea FC Women supporters.", icon: "🔵" },
  { name: "FC Barcelona Femení", slug: "barcelona-femeni", description: "Blaugrana hearts, one team. Visca el Barça!", icon: "🔵🔴" },
  { name: "USWNT", slug: "uswnt", description: "One nation. One team. All for the stars and stripes.", icon: "🇺🇸" },
];

function timeAgo(value: string) {
  const minutes = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
  if (minutes < 60) return `${minutes}m ago`;
  if (minutes < 1440) return `${Math.floor(minutes / 60)}h ago`;
  return `${Math.floor(minutes / 1440)}d ago`;
}

function slugify(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

function App() {
  const [communities, setCommunities] = useState<Community[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [feed, setFeed] = useState<FeedPost[]>([]);
  const [user, setUser] = useState<User | null>(null);
  const [joinedIds, setJoinedIds] = useState<string[]>([]);
  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [postText, setPostText] = useState("");
  const [commentText, setCommentText] = useState<Record<string, string>>({});

  const selected = communities.find((community) => community.id === selectedId) ?? null;
  const displayName = user?.displayName ?? "";
  const joined = selected ? joinedIds.includes(selected.id) : false;
  const filtered = useMemo(() =>
    communities.filter((community) =>
      `${community.name} ${community.description}`.toLowerCase().includes(search.toLowerCase())),
  [communities, search]);

  async function loadCommunities(selectFirst = false) {
    try {
      const data = await api.communities();
      setCommunities(data);
      if (selectFirst && data.length) setSelectedId(data[0].id);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not load communities");
    } finally {
      setLoading(false);
    }
  }

  async function refreshMemberships() {
    if (!user) {
      setJoinedIds([]);
      return;
    }
    try {
      setJoinedIds(await api.myMemberships());
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not load memberships");
    }
  }

  async function loadFeed(communityId: string) {
    try {
      const posts = await api.posts(communityId);
      const withComments = await Promise.all(posts.map(async (post) => ({
        ...post,
        comments: await api.comments(post.id),
      })));
      setFeed(withComments);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not load posts");
    }
  }

  useEffect(() => {
    void (async () => {
      try {
        await api.csrf();
        try {
          setUser(await api.me());
        } catch (error) {
          if (!(error instanceof ApiError) || error.status !== 401) throw error;
        }
        await loadCommunities(true);
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "Could not connect to the community service");
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => { void refreshMemberships(); }, [user]);

  useEffect(() => {
    if (selectedId) void loadFeed(selectedId);
    else setFeed([]);
  }, [selectedId]);

  function openAuthentication() {
    setAuthMode("login");
    setShowProfile(true);
  }

  function openCreateCommunity() {
    if (!user) {
      openAuthentication();
      setMessage("Sign in to create a community.");
      return;
    }
    setShowCreate(true);
  }

  async function toggleMembership() {
    if (!selected) return;
    if (!user) {
      openAuthentication();
      return;
    }
    setSaving(true);
    try {
      if (joined) {
        await api.leave(selected.id);
      } else {
        await api.join(selected.id);
      }
      await refreshMemberships();
      await loadCommunities();
      setMessage(joined ? "You left the community." : `Welcome to ${selected.name}!`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not update membership");
    } finally {
      setSaving(false);
    }
  }

  async function submitPost(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || !postText.trim()) return;
    if (!user) {
      openAuthentication();
      return;
    }
    setSaving(true);
    try {
      await api.createPost(selected.id, postText.trim());
      setPostText("");
      await loadFeed(selected.id);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not publish post");
    } finally {
      setSaving(false);
    }
  }

  async function submitComment(event: FormEvent<HTMLFormElement>, postId: string) {
    event.preventDefault();
    const content = commentText[postId]?.trim();
    if (!content) return;
    if (!user) {
      openAuthentication();
      return;
    }
    try {
      await api.createComment(postId, content);
      setCommentText((current) => ({ ...current, [postId]: "" }));
      if (selectedId) await loadFeed(selectedId);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not add comment");
    }
  }

  async function submitAuthentication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const password = String(form.get("password") ?? "");
    setSaving(true);
    try {
      const authenticatedUser = authMode === "register"
        ? await api.register({
          email,
          password,
          displayName: String(form.get("displayName") ?? "").trim(),
        })
        : await api.login({ email, password });
      setUser(authenticatedUser);
      setShowProfile(false);
      setMessage(`Welcome${authMode === "register" ? " to the touchline" : " back"}, ${authenticatedUser.displayName}!`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not sign in");
    } finally {
      setSaving(false);
    }
  }

  async function signOut() {
    try {
      await api.logout();
      setUser(null);
      setJoinedIds([]);
      setShowProfile(false);
      setMessage("You have signed out.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not sign out");
    }
  }

  async function createCommunity(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!user) {
      openAuthentication();
      return;
    }
    const form = new FormData(event.currentTarget);
    const name = String(form.get("name") ?? "").trim();
    const description = String(form.get("description") ?? "").trim();
    if (!name || !description) return;
    setSaving(true);
    try {
      const community = await api.createCommunity({ name, description, slug: slugify(name) });
      setShowCreate(false);
      await loadCommunities();
      setSelectedId(community.id);
      setMessage(`Your ${community.name} community is ready.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not create community");
    } finally {
      setSaving(false);
    }
  }

  const initial = displayName.trim().charAt(0).toUpperCase() || "Y";

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setSelectedId(null)} aria-label="Touchline home">
          <span className="brand-ball">⚽</span>
          <span className="brand-type">touchline<span>.</span><small>WOMEN'S FOOTBALL COMMUNITY</small></span>
        </button>
        <div className="top-search"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Find your football people..." /></div>
        <div className="top-actions">
          <a className="api-link" href="/swagger-ui.html">API docs <ArrowDownRight size={14} /></a>
          <button className="icon-btn notification-btn" aria-label="Notifications (coming soon)" title="Notifications are coming soon"><Bell size={19} /><i /></button>
          <button className="profile-button" onClick={() => user ? setShowProfile(true) : openAuthentication()} title={displayName || "Sign in"}>
            <span className="avatar">{initial}</span><span className="profile-name">{displayName || "Sign in"}</span><ChevronDown size={15} />
          </button>
        </div>
      </header>

      <main className="page">
        <section className="hero">
          <div className="hero-copy">
            <div className="eyebrow"><span className="live-dot" /> THE GAME IS BETTER TOGETHER</div>
            <h1>For the love<br />of <em>the game.</em></h1>
            <p>A home for every fan, every club, every conversation. Pull up a seat on the touchline.</p>
            <button className="hero-cta" onClick={() => document.getElementById("community-list")?.scrollIntoView({ behavior: "smooth" })}>
              Find your community <ArrowRight size={16} />
            </button>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="sun-disc" /><div className="orbit orbit-one" /><div className="orbit orbit-two" />
            <div className="hero-ball">⚽</div><span className="sparkle sparkle-a">✳</span><span className="sparkle sparkle-b">✳</span>
            <div className="hero-caption">ONE GAME. <b>ENDLESS STORIES.</b></div>
          </div>
          <div className="hero-index">EST. FOR THE FANS <span>✦</span> ALWAYS FOR THE GAME</div>
        </section>

        <div className="main-grid">
          <aside className="sidebar" id="community-list">
            <div className="section-title"><span>YOUR TOUCHLINE</span><button className="icon-btn small" onClick={openCreateCommunity} aria-label="Create community"><Plus size={18} /></button></div>
            <button className={`side-link ${!selected ? "active" : ""}`} onClick={() => setSelectedId(null)}><Club size={17} /> Discover</button>
            <div className="subheading">COMMUNITIES <span>{communities.length}</span></div>
            <div className="community-menu">
              {filtered.map((community) => {
                const config = initialCommunities.find((item) => item.slug === community.slug);
                return (
                  <button key={community.id} className={`community-link ${selectedId === community.id ? "active" : ""}`} onClick={() => setSelectedId(community.id)}>
                    <span className="club-icon">{config?.icon ?? "⚽"}</span><span className="club-name">{community.name}</span>
                    {joinedIds.includes(community.id) && <span className="joined-check"><Check size={12} /></span>}
                  </button>
                );
              })}
              {!loading && filtered.length === 0 && <p className="empty-sidebar">No communities match that search.</p>}
            </div>
            <button className="create-link" onClick={openCreateCommunity}><span><Plus size={16} /></span> Start a community</button>
            <div className="sidebar-card">
              <div className="sidebar-card-icon"><Sparkles size={18} /></div>
              <b>This is your space.</b>
              <p>Every voice belongs on the touchline. Keep it kind, keep it about the game.</p>
              <a href="#community-guidelines">Community guidelines <ArrowRight size={13} /></a>
            </div>
            <div className="sidebar-footer">MADE FOR THE LOVE OF FOOTBALL <span>♥</span><small>© 2026 TOUCHLINE COMMUNITY</small></div>
          </aside>

          <section className="content">
            {!selected ? (
              <div className="discover-view">
                <div className="content-heading">
                  <div><div className="eyebrow dark">THE WORLD OF WOMEN'S FOOTBALL</div><h2>Find your <em>people.</em></h2><p>Big match energy. Little moments. All of it belongs here.</p></div>
                  <button className="outline-button" onClick={openCreateCommunity}><Plus size={16} /> Create a community</button>
                </div>
                <div className="stats-strip"><div><strong>{communities.length.toString().padStart(2, "0")}</strong><span>COMMUNITIES</span></div><i /><div><strong>{communities.reduce((total, item) => total + item.memberCount, 0).toString().padStart(2, "0")}</strong><span>FANS ON THE TOUCHLINE</span></div><div className="stats-note">Your club. Your people. <b>Your game.</b></div></div>
                <div className="discover-label"><span>THE FAN SECTIONS</span><span>{filtered.length} COMMUNITIES <ArrowDownRight size={13} /></span></div>
                {loading ? <div className="loading">Setting up your touchline...</div> : (
                  <div className="community-cards">
                    {filtered.map((community, index) => {
                      const config = initialCommunities.find((item) => item.slug === community.slug);
                      return <button className="community-card" key={community.id} onClick={() => setSelectedId(community.id)}>
                        <div className={`card-art card-art-${index % 4}`}><span>{config?.icon ?? "⚽"}</span><div className="card-art-lines" /><span className="card-arrow"><ArrowRight size={17} /></span></div>
                        <div className="card-info"><div className="card-name"><h3>{community.name}</h3>{joinedIds.includes(community.id) && <span className="member-pill">JOINED</span>}</div><p>{community.description}</p><div className="card-meta"><span><Users size={14} /> {community.memberCount} {community.memberCount === 1 ? "fan" : "fans"}</span><span>STEP INTO THE STANDS <ArrowRight size={13} /></span></div></div>
                      </button>;
                    })}
                    {!filtered.length && <div className="no-results">No communities found. Start one for your team.</div>}
                  </div>
                )}
                <div className="bottom-banner"><span className="banner-star">✳</span><div><b>There's room for one more.</b><p>Don't see your team? Bring your people together.</p></div><button onClick={openCreateCommunity}>Start a community <ArrowRight size={15} /></button></div>
              </div>
            ) : (
              <div className="feed-view">
                <button className="back-link" onClick={() => setSelectedId(null)}><ArrowLeft size={15} /> ALL COMMUNITIES</button>
                <div className="community-banner">
                  <div className="banner-decoration">✳</div>
                  <div className="banner-team-icon">{initialCommunities.find((item) => item.slug === selected.slug)?.icon ?? "⚽"}</div>
                  <div className="banner-copy"><div className="eyebrow">THE FAN SECTION</div><h2>{selected.name}</h2><p>{selected.description}</p><span><Users size={14} /> {selected.memberCount} on the touchline</span></div>
                  <button className={`join-button ${joined ? "joined" : ""}`} onClick={() => void toggleMembership()} disabled={saving}>{joined ? <><Check size={15} /> Joined</> : <><Plus size={16} /> Join community</>}</button>
                </div>
                <div className="feed-heading"><div><div className="eyebrow dark">THE CONVERSATION</div><h2>Latest from <em>the stands.</em></h2></div><span className="feed-live"><i /> OPEN DISCUSSION</span></div>
                {joined && <form className="composer" onSubmit={(event) => void submitPost(event)}><div className="avatar composer-avatar">{initial}</div><div className="composer-body"><textarea value={postText} onChange={(event) => setPostText(event.target.value)} placeholder={`What's happening, ${displayName || "fan"}?`} maxLength={2000} /><div className="composer-footer"><span>Bring the match-day energy. Keep it kind.</span><button disabled={saving || !postText.trim()}><Send size={14} /> {saving ? "Posting..." : "Share with the stands"}</button></div></div></form>}
                {!joined && <div className="join-nudge"><Users size={18} /><span><b>Want to join the conversation?</b> Join this community to share a post.</span><button onClick={() => void toggleMembership()}>Join now <ArrowRight size={14} /></button></div>}
                {feed.length ? <div className="post-list">{feed.map((post) => (
                  <article className="post-card" key={post.id}>
                    <div className="post-top"><div className="avatar post-avatar">{post.authorName.charAt(0).toUpperCase()}</div><div className="post-author"><b>{post.authorName}</b><span>in <strong>{selected.name}</strong> · {timeAgo(post.createdAt)}</span></div><button className="post-menu" title="More options">···</button></div>
                    <p className="post-content">{post.content}</p>
                    <div className="post-actions"><span><MessageCircle size={15} /> {post.comments.length} {post.comments.length === 1 ? "reply" : "replies"}</span><span><Heart size={15} /> Good vibes only</span></div>
                    <div className="comment-list">{post.comments.map((comment) => <div className="comment" key={comment.id}><span className="comment-avatar">{comment.authorName.charAt(0).toUpperCase()}</span><div><p><b>{comment.authorName}</b> {comment.content}</p><small>{timeAgo(comment.createdAt)}</small></div></div>)}</div>
                    {joined && <form className="comment-form" onSubmit={(event) => void submitComment(event, post.id)}><div className="avatar comment-self">{initial}</div><input value={commentText[post.id] ?? ""} onChange={(event) => setCommentText((current) => ({ ...current, [post.id]: event.target.value }))} placeholder="Add to the conversation..." maxLength={1000} /><button disabled={!commentText[post.id]?.trim()} aria-label="Send comment"><Send size={16} /></button></form>}
                  </article>
                ))}</div> : <div className="empty-feed"><div className="empty-icon"><MessageCircle size={23} /></div><h3>Fresh pitch. No posts yet.</h3><p>Be the first to get the conversation started.</p></div>}
                <div className="guideline-note" id="community-guidelines"><Shield size={16} /><span>Make this a place everyone wants to be. <b>Be respectful, celebrate the game, and look out for one another.</b></span><CircleHelp size={15} /></div>
              </div>
            )}
          </section>

          <aside className="right-rail">
            <div className="rail-card match-card"><div className="rail-heading"><span>THE BIG PICTURE</span><span className="tiny-star">✳</span></div><div className="match-illustration"><div className="stadium-circle" /><span className="match-ball">⚽</span><span className="match-spark">✦</span></div><div className="match-copy"><b>It's more than 90 minutes.</b><p>It's the stories, the songs, the ones you share it with.</p></div><div className="match-foot">SAME LOVE. <b>NEW STORIES.</b></div></div>
            <div className="rail-card trending-card"><div className="rail-heading"><span>ON THE TOUCHLINE</span><span className="live-label"><i /> LIVE</span></div><div className="trending-item"><span className="trend-num">01</span><div><b>Women's football is for everyone</b><small>THE COMMUNITY · A GOOD PLACE TO START</small></div></div><div className="trending-item"><span className="trend-num">02</span><div><b>Share the match-day feeling</b><small>YOUR PEOPLE ARE HERE · JOIN IN</small></div></div><div className="trending-item"><span className="trend-num">03</span><div><b>Start a fan section for your club</b><small>MAKE IT YOURS · EVERY TEAM WELCOME</small></div></div></div>
            <div className="rail-quote"><span>“</span><p>The beautiful game is even more beautiful when we make space for everyone.</p><small>A LITTLE TOUCHLINE REMINDER</small></div>
          </aside>
        </div>
      </main>

      {message && <div className="toast" role="status">{message}<button onClick={() => setMessage("")} aria-label="Dismiss"><X size={15} /></button></div>}
      {showCreate && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowCreate(false); }}><form className="modal" onSubmit={(event) => void createCommunity(event)}><button type="button" className="modal-close" onClick={() => setShowCreate(false)} aria-label="Close"><X size={19} /></button><div className="modal-icon"><Plus size={20} /></div><div className="eyebrow dark">MAKE SOME ROOM IN THE STANDS</div><h2>Start your <em>fan section.</em></h2><p>Every club deserves a place to call home. Get your people together.</p><label>COMMUNITY NAME<input autoFocus name="name" placeholder="e.g. Arsenal Women" maxLength={80} required /></label><label>WHAT'S THIS COMMUNITY ABOUT?<textarea name="description" placeholder="Tell fans what brings this community together..." maxLength={500} required /></label><button className="modal-submit" disabled={saving}>{saving ? "Creating..." : "Create your community"} <ArrowRight size={16} /></button></form></div>}
      {showProfile && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowProfile(false); }}>
        <div className="modal profile-modal">
          <button type="button" className="modal-close" onClick={() => setShowProfile(false)} aria-label="Close"><X size={19} /></button>
          <div className="modal-icon"><LogIn size={20} /></div>
          {user ? <>
            <div className="eyebrow dark">YOUR TOUCHLINE ACCOUNT</div>
            <h2>Good to have you, <em>{user.displayName}.</em></h2>
            <p>{user.email}</p>
            <button type="button" className="modal-submit" onClick={() => void signOut()}>Sign out <ArrowRight size={16} /></button>
          </> : <>
            <div className="eyebrow dark">WELCOME TO THE TOUCHLINE</div>
            <h2>{authMode === "register" ? <>Join the <em>community.</em></> : <>Welcome <em>back.</em></>}</h2>
            <p>{authMode === "register" ? "Create an account to find your people and join the conversation." : "Sign in to pick up where you left off."}</p>
            <div className="auth-tabs">
              <button type="button" className={authMode === "login" ? "active" : ""} onClick={() => setAuthMode("login")}>Sign in</button>
              <button type="button" className={authMode === "register" ? "active" : ""} onClick={() => setAuthMode("register")}>Create account</button>
            </div>
            <form key={authMode} onSubmit={(event) => void submitAuthentication(event)}>
              {authMode === "register" && <label>DISPLAY NAME<input autoFocus name="displayName" maxLength={40} placeholder="e.g. Alex" autoComplete="nickname" required /></label>}
              <label>EMAIL ADDRESS<input autoFocus={authMode === "login"} name="email" type="email" maxLength={254} placeholder="you@example.com" autoComplete="email" required /></label>
              <label>PASSWORD<input name="password" type="password" minLength={authMode === "register" ? 8 : undefined} maxLength={72} placeholder={authMode === "register" ? "At least 8 characters" : "Your password"} autoComplete={authMode === "register" ? "new-password" : "current-password"} required /></label>
              <button className="modal-submit" disabled={saving}>{saving ? "Please wait..." : authMode === "register" ? "Create account" : "Sign in"} <ArrowRight size={16} /></button>
            </form>
            <small className="demo-notice">Passwords are stored as BCrypt hashes; sign-in uses a secure server session.</small>
          </>}
        </div>
      </div>}
    </div>
  );
}

export default App;
