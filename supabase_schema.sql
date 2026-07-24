-- =====================================================================
--  Virtual Cam (VCam) — Supabase Database Schema
--  Run this in Supabase SQL Editor after creating your project
-- =====================================================================

-- Enable UUID extension (usually already enabled in Supabase)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─────────────────────────────────────────────────────────────────────
-- 1. Profiles (extends auth.users created automatically by Supabase Auth)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.profiles (
    id          UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email       TEXT        NOT NULL,
    full_name   TEXT,
    is_admin    BOOLEAN     NOT NULL DEFAULT FALSE,
    is_banned   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Auto-create profile on new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name)
    VALUES (
        NEW.id,
        NEW.email,
        NEW.raw_user_meta_data->>'full_name'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ─────────────────────────────────────────────────────────────────────
-- 2. Subscription Plans
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.subscription_plans (
    id            SERIAL       PRIMARY KEY,
    name          TEXT         NOT NULL,
    name_ar       TEXT,                             -- Arabic display name
    duration_days INTEGER,                          -- NULL = permanent / lifetime
    price         DECIMAL(10,2) NOT NULL,
    currency      TEXT         NOT NULL DEFAULT 'USD',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Default plans (can be edited from admin panel)
INSERT INTO public.subscription_plans (name, name_ar, duration_days, price, currency) VALUES
    ('GOLD',      'ذهبي - 10 أيام',  10,   30.00, 'USD'),
    ('PLATINUM',  'بلاتيني - 30 يوم', 30,   70.00, 'USD'),
    ('LIFETIME',  'دائم - مدى الحياة', NULL, 100.00, 'USD')
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────
-- 3. Payment Methods
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.payment_methods (
    id               SERIAL  PRIMARY KEY,
    name             TEXT    NOT NULL,
    name_ar          TEXT,
    address          TEXT    NOT NULL,       -- Account number / wallet address
    instructions     TEXT,                  -- EN instructions
    instructions_ar  TEXT,                  -- AR instructions
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Default payment methods
INSERT INTO public.payment_methods (name, name_ar, address, instructions_ar) VALUES
    ('Sham Cash',     'شام كاش',     '0991234567',  'أرسل المبلغ المحدد إلى الرقم أعلاه، ثم أدخل رقم العملية في الحقل المخصص'),
    ('Syriatel Cash', 'سيريتل كاش', '0941234567',  'حوّل المبلغ عبر تطبيق سيريتل كاش إلى الرقم أعلاه، ثم أدخل رقم العملية'),
    ('USDT (TRC20)',  'USDT تيثر',  'TYourWalletAddressHere', 'أرسل المبلغ بعملة USDT على شبكة TRC20 إلى المحفظة أعلاه، ثم أدخل رقم التحويل (TXID)')
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────
-- 4. Subscriptions (active / expired / cancelled)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id          SERIAL       PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan_id     INTEGER      NOT NULL REFERENCES public.subscription_plans(id),
    status      TEXT         NOT NULL DEFAULT 'active'   -- active | expired | cancelled
                             CHECK (status IN ('active', 'expired', 'cancelled')),
    starts_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,                             -- NULL = permanent
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index for fast lookup of active subscriptions per user
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status
    ON public.subscriptions (user_id, status);

-- ─────────────────────────────────────────────────────────────────────
-- 5. Subscription Requests (pending admin approval)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.subscription_requests (
    id                  SERIAL       PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan_id             INTEGER      NOT NULL REFERENCES public.subscription_plans(id),
    payment_method_id   INTEGER      NOT NULL REFERENCES public.payment_methods(id),
    transaction_number  TEXT         NOT NULL,
    amount              DECIMAL(10,2) NOT NULL,
    status              TEXT         NOT NULL DEFAULT 'pending'
                                     CHECK (status IN ('pending', 'approved', 'rejected')),
    admin_note          TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index for pending requests
CREATE INDEX IF NOT EXISTS idx_subscription_requests_status
    ON public.subscription_requests (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_requests_user
    ON public.subscription_requests (user_id, created_at DESC);

-- ─────────────────────────────────────────────────────────────────────
-- 6. Row Level Security (RLS) Policies
-- ─────────────────────────────────────────────────────────────────────

-- Enable RLS on all tables
ALTER TABLE public.profiles              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscription_plans    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_methods       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscription_requests ENABLE ROW LEVEL SECURITY;

-- Drop existing policies to avoid conflicts
DROP POLICY IF EXISTS "Users can read own profile"           ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile"         ON public.profiles;
DROP POLICY IF EXISTS "Admins can read all profiles"         ON public.profiles;
DROP POLICY IF EXISTS "Admins can update profiles"           ON public.profiles;
DROP POLICY IF EXISTS "Anyone can read active plans"         ON public.subscription_plans;
DROP POLICY IF EXISTS "Admins manage plans"                  ON public.subscription_plans;
DROP POLICY IF EXISTS "Anyone can read active methods"       ON public.payment_methods;
DROP POLICY IF EXISTS "Admins manage methods"                ON public.payment_methods;
DROP POLICY IF EXISTS "Users can read own subscriptions"     ON public.subscriptions;
DROP POLICY IF EXISTS "Admins manage subscriptions"          ON public.subscriptions;
DROP POLICY IF EXISTS "Users can read own requests"          ON public.subscription_requests;
DROP POLICY IF EXISTS "Users can insert requests"            ON public.subscription_requests;
DROP POLICY IF EXISTS "Admins manage requests"               ON public.subscription_requests;

-- Helper function: is current user admin?
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
    SELECT COALESCE(
        (SELECT is_admin FROM public.profiles WHERE id = auth.uid()),
        FALSE
    );
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

-- Profiles policies
CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (id = auth.uid() OR public.is_admin());

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

CREATE POLICY "Admins can update profiles"
    ON public.profiles FOR UPDATE
    USING (public.is_admin());

CREATE POLICY "Allow profile insert on signup"
    ON public.profiles FOR INSERT
    WITH CHECK (id = auth.uid());

-- Plans policies
CREATE POLICY "Anyone can read active plans"
    ON public.subscription_plans FOR SELECT
    USING (is_active = TRUE OR public.is_admin());

CREATE POLICY "Admins manage plans"
    ON public.subscription_plans FOR ALL
    USING (public.is_admin());

-- Payment methods policies
CREATE POLICY "Anyone can read active methods"
    ON public.payment_methods FOR SELECT
    USING (is_active = TRUE OR public.is_admin());

CREATE POLICY "Admins manage methods"
    ON public.payment_methods FOR ALL
    USING (public.is_admin());

-- Subscriptions policies
CREATE POLICY "Users can read own subscriptions"
    ON public.subscriptions FOR SELECT
    USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "Admins manage subscriptions"
    ON public.subscriptions FOR ALL
    USING (public.is_admin());

-- Subscription requests policies
CREATE POLICY "Users can read own requests"
    ON public.subscription_requests FOR SELECT
    USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "Users can insert requests"
    ON public.subscription_requests FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Admins manage requests"
    ON public.subscription_requests FOR ALL
    USING (public.is_admin());

-- ─────────────────────────────────────────────────────────────────────
-- 7. Useful views (optional but helpful for admin queries)
-- ─────────────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW public.admin_subscription_overview AS
SELECT
    sr.id,
    sr.created_at,
    p.email         AS user_email,
    p.full_name     AS user_name,
    sp.name_ar      AS plan_name,
    pm.name_ar      AS payment_method,
    sr.transaction_number,
    sr.amount,
    sr.status
FROM public.subscription_requests sr
JOIN public.profiles          p  ON p.id  = sr.user_id
JOIN public.subscription_plans sp ON sp.id = sr.plan_id
JOIN public.payment_methods   pm ON pm.id = sr.payment_method_id
ORDER BY sr.created_at DESC;

-- =====================================================================
--  SETUP COMPLETE!
--  Next steps:
--  1. Go to Supabase → Authentication → Settings → Email Auth → Enable
--  2. (Optional) Enable Google OAuth under Auth → Providers → Google
--  3. Add SUPABASE_URL and SUPABASE_ANON_KEY as GitHub Actions secrets
--  4. Add GOOGLE_WEB_CLIENT_ID as a GitHub Actions secret (if using Google)
--  5. To make a user admin: UPDATE public.profiles SET is_admin=TRUE WHERE email='your@email.com';
-- =====================================================================
