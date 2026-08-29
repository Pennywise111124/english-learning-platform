/**
 * mockApi.js — LinguistAI Mock API Layer
 * All functions mirror the real API contract.
 * To switch to real API: replace api.js internals, keep these function signatures.
 */

// ─── Delay helper ────────────────────────────────────────────────────────────
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));
const rand  = ()   => 300 + Math.floor(Math.random() * 200); // 300-500ms

// ─── Mock Data ────────────────────────────────────────────────────────────────

const MOCK_TOPICS = [
  { id: 1, title: "Travel & Transportation", description: "Learn essential vocabulary for airports, hotels, and navigating new cities.", level: "BEGINNER",     imageUrl: "https://picsum.photos/seed/travel/400/240"      },
  { id: 2, title: "Food & Dining",           description: "Master restaurant conversations, menu vocabulary, and cooking terms.",             level: "BEGINNER",     imageUrl: "https://picsum.photos/seed/food/400/240"        },
  { id: 3, title: "Business Communication",  description: "Professional English for meetings, emails, and workplace interactions.",           level: "INTERMEDIATE", imageUrl: "https://picsum.photos/seed/business/400/240"    },
  { id: 4, title: "Technology & Innovation", description: "Discuss the latest tech trends, programming concepts, and digital tools.",         level: "INTERMEDIATE", imageUrl: "https://picsum.photos/seed/tech/400/240"        },
  { id: 5, title: "Health & Medicine",       description: "Medical terminology, doctor visits, and health-related conversations.",            level: "INTERMEDIATE", imageUrl: "https://picsum.photos/seed/health/400/240"      },
  { id: 6, title: "Literature & Arts",       description: "Explore English literature, art criticism, and cultural discussions.",             level: "ADVANCED",     imageUrl: "https://picsum.photos/seed/arts/400/240"        },
  { id: 7, title: "Environmental Science",   description: "Climate change, sustainability, and environmental policy vocabulary.",             level: "ADVANCED",     imageUrl: "https://picsum.photos/seed/environment/400/240" },
  { id: 8, title: "Daily Conversations",     description: "Everyday English for shopping, weather, small talk, and social situations.",      level: "BEGINNER",     imageUrl: "https://picsum.photos/seed/daily/400/240"       },
];

const MOCK_FLASHCARDS = {
  1: [
    { id: 1,  word: "airport",      meaning: "sân bay",                        example: "We arrived at the airport two hours early.",          imageUrl: "https://picsum.photos/seed/airport/200/150",     audioUrl: null },
    { id: 2,  word: "departure",    meaning: "khởi hành / xuất phát",          example: "The departure gate is on the second floor.",          imageUrl: "https://picsum.photos/seed/departure/200/150",   audioUrl: null },
    { id: 3,  word: "boarding pass",meaning: "thẻ lên máy bay",                example: "Please have your boarding pass ready.",               imageUrl: "https://picsum.photos/seed/boarding/200/150",    audioUrl: null },
    { id: 4,  word: "customs",      meaning: "hải quan",                        example: "All passengers must go through customs.",             imageUrl: "https://picsum.photos/seed/customs/200/150",     audioUrl: null },
    { id: 5,  word: "itinerary",    meaning: "lịch trình chuyến đi",           example: "The travel agent sent us a detailed itinerary.",      imageUrl: "https://picsum.photos/seed/itinerary/200/150",   audioUrl: null },
  ],
  2: [
    { id: 6,  word: "appetizer",    meaning: "món khai vị",                    example: "We ordered soup as an appetizer.",                    imageUrl: "https://picsum.photos/seed/appetizer/200/150",   audioUrl: null },
    { id: 7,  word: "cuisine",      meaning: "ẩm thực",                        example: "I love Italian cuisine.",                             imageUrl: "https://picsum.photos/seed/cuisine/200/150",     audioUrl: null },
    { id: 8,  word: "reservation",  meaning: "đặt chỗ trước",                  example: "Do you have a reservation?",                          imageUrl: "https://picsum.photos/seed/reservation/200/150", audioUrl: null },
    { id: 9,  word: "menu",         meaning: "thực đơn",                       example: "Can I see the menu, please?",                         imageUrl: "https://picsum.photos/seed/menu/200/150",        audioUrl: null },
    { id: 10, word: "bill",         meaning: "hóa đơn",                        example: "Could we have the bill, please?",                     imageUrl: "https://picsum.photos/seed/bill/200/150",        audioUrl: null },
  ],
  3: [
    { id: 11, word: "agenda",       meaning: "chương trình nghị sự",           example: "Please review the meeting agenda before joining.",     imageUrl: "https://picsum.photos/seed/agenda/200/150",      audioUrl: null },
    { id: 12, word: "deadline",     meaning: "hạn chót",                       example: "The deadline for this project is Friday.",             imageUrl: "https://picsum.photos/seed/deadline/200/150",    audioUrl: null },
    { id: 13, word: "negotiate",    meaning: "đàm phán",                       example: "We need to negotiate the contract terms.",             imageUrl: "https://picsum.photos/seed/negotiate/200/150",   audioUrl: null },
    { id: 14, word: "stakeholder",  meaning: "bên liên quan",                  example: "All stakeholders must approve the plan.",              imageUrl: "https://picsum.photos/seed/stakeholder/200/150", audioUrl: null },
    { id: 15, word: "feedback",     meaning: "phản hồi / ý kiến đóng góp",    example: "I appreciate your constructive feedback.",             imageUrl: "https://picsum.photos/seed/feedback/200/150",    audioUrl: null },
  ],
};

const DEFAULT_FLASHCARDS = (topicId) => [
  { id: topicId*10+1, word: "vocabulary",    meaning: "từ vựng",             example: "Building vocabulary takes daily practice.",                imageUrl: `https://picsum.photos/seed/vocab${topicId}/200/150`,   audioUrl: null },
  { id: topicId*10+2, word: "pronunciation", meaning: "phát âm",             example: "Good pronunciation helps people understand you.",          imageUrl: `https://picsum.photos/seed/pron${topicId}/200/150`,    audioUrl: null },
  { id: topicId*10+3, word: "fluency",       meaning: "sự trôi chảy",        example: "She speaks English with great fluency.",                   imageUrl: `https://picsum.photos/seed/fluency${topicId}/200/150`, audioUrl: null },
  { id: topicId*10+4, word: "comprehension", meaning: "sự đọc hiểu",         example: "Reading improves your comprehension skills.",              imageUrl: `https://picsum.photos/seed/comp${topicId}/200/150`,    audioUrl: null },
  { id: topicId*10+5, word: "expression",    meaning: "cách diễn đạt",       example: "That is a common English expression.",                     imageUrl: `https://picsum.photos/seed/expr${topicId}/200/150`,    audioUrl: null },
];

const MOCK_QUIZZES = {
  1: [ { id: 1, title: "Travel Vocabulary Quiz" },      { id: 2, title: "Transportation Phrases Quiz" } ],
  2: [ { id: 3, title: "Food & Restaurant Quiz" },      { id: 4, title: "Cooking Vocabulary Quiz" } ],
  3: [ { id: 5, title: "Business English Basics" },     { id: 6, title: "Meeting & Email Phrases" } ],
  4: [ { id: 7, title: "Tech Vocabulary Quiz" },        { id: 8, title: "Digital Tools Quiz" } ],
  5: [ { id: 9, title: "Medical Terminology Quiz" },    { id: 10, title: "Health Phrases Quiz" } ],
  6: [ { id: 11, title: "Literature Vocab Quiz" },      { id: 12, title: "Art & Culture Quiz" } ],
  7: [ { id: 13, title: "Environment Vocab Quiz" },     { id: 14, title: "Climate Policy Quiz" } ],
  8: [ { id: 15, title: "Daily Conversation Quiz" },    { id: 16, title: "Small Talk Quiz" } ],
};

const MOCK_QUIZ_DETAILS = {
  1: {
    id: 1, title: "Travel Vocabulary Quiz",
    questions: [
      { id: 1,  question: "What is 'airport' in Vietnamese?",               options: ["sân bay", "khách sạn", "nhà ga", "bến xe"] },
      { id: 2,  question: "Choose the correct meaning of 'departure':",     options: ["đến nơi", "khởi hành", "quá cảnh", "nhập cảnh"] },
      { id: 3,  question: "'Boarding pass' means:",                          options: ["vé tàu", "hộ chiếu", "thẻ lên máy bay", "visa"] },
      { id: 4,  question: "What does 'customs' mean?",                      options: ["phong tục", "hải quan", "hành lý", "cửa khẩu"] },
      { id: 5,  question: "'Itinerary' is best described as:",               options: ["vé máy bay", "bản đồ", "lịch trình chuyến đi", "bảo hiểm du lịch"] },
    ]
  },
  2: {
    id: 2, title: "Transportation Phrases Quiz",
    questions: [
      { id: 6,  question: "How do you say 'one-way ticket'?",               options: ["vé khứ hồi", "vé một chiều", "vé đứng", "vé hạng nhất"] },
      { id: 7,  question: "What is a 'layover'?",                           options: ["bay thẳng", "dừng chân tạm thời", "hạ cánh khẩn cấp", "chuyến bay đêm"] },
      { id: 8,  question: "'Carry-on luggage' means:",                      options: ["hành lý ký gửi", "hành lý xách tay", "hàng hóa", "đồ dễ vỡ"] },
      { id: 9,  question: "What does 'terminal' refer to?",                 options: ["bến tàu", "trạm dừng", "nhà ga sân bay", "đường ray"] },
      { id: 10, question: "What is 'transit visa'?",                        options: ["visa du học", "visa quá cảnh", "visa định cư", "visa thương mại"] },
    ]
  },
};

const GENERIC_QUIZ_DETAIL = (quizId) => ({
  id: Number(quizId), title: `Quiz #${quizId}`,
  questions: [1,2,3,4,5].map(n => ({
    id: Number(quizId) * 10 + n,
    question: `Question ${n}: Which sentence is grammatically correct?`,
    options: ["She don't like coffee.", "She doesn't like coffee.", "She not like coffee.", "She isn't like coffee."]
  }))
});

const MOCK_CONVERSATIONS = [
  { id: 1, title: "Small talk practice",         updatedAt: "2026-08-29T10:00:00Z" },
  { id: 2, title: "Travel planning conversation", updatedAt: "2026-08-28T15:30:00Z" },
  { id: 3, title: "Job interview preparation",    updatedAt: "2026-08-27T09:00:00Z" },
];

const MOCK_MESSAGES = {
  1: [
    { id: 1, sender: "USER", content: "How are you today?",                      correction: null,                              explanation: null,                                                                                                           createdAt: "2026-08-29T09:55:00Z" },
    { id: 2, sender: "AI",   content: "I'm doing great, thank you for asking! How about you? Did you have a good weekend?",  correction: null, explanation: null,                                                                                           createdAt: "2026-08-29T09:55:05Z" },
    { id: 3, sender: "USER", content: "I goes to the market yesterday.",          correction: "I went to the market yesterday.", explanation: "Use simple past tense 'went' instead of present tense 'goes' when describing a past event.",                  createdAt: "2026-08-29T09:56:00Z" },
    { id: 4, sender: "AI",   content: "That sounds fun! What did you buy at the market? I noticed a small grammar point — you wrote 'I goes' but it should be 'I went' since we're talking about yesterday.", correction: null, explanation: null,         createdAt: "2026-08-29T09:56:05Z" },
    { id: 5, sender: "USER", content: "I buyed some vegetables and fruits.",      correction: "I bought some vegetables and fruits.", explanation: "'Buy' is an irregular verb. Its past tense is 'bought', not 'buyed'.",                                    createdAt: "2026-08-29T09:57:00Z" },
    { id: 6, sender: "AI",   content: "Great! Eating fresh produce is so healthy. By the way, 'buy' is an irregular verb — the past tense is 'bought', not 'buyed'. You're making great progress!", correction: null, explanation: null,                 createdAt: "2026-08-29T09:57:05Z" },
  ],
  2: [
    { id: 7, sender: "USER", content: "I want to plan a trip to Japan.",          correction: null, explanation: null, createdAt: "2026-08-28T15:28:00Z" },
    { id: 8, sender: "AI",   content: "How exciting! Japan is a wonderful destination. When are you planning to go, and is this your first visit?", correction: null, explanation: null, createdAt: "2026-08-28T15:28:05Z" },
  ],
  3: [],
};

const MOCK_PROGRESS = [
  { topicId: 1, topicTitle: "Travel & Transportation", status: "COMPLETED",   progressPercent: 100, updatedAt: "2026-08-25T10:00:00Z" },
  { topicId: 2, topicTitle: "Food & Dining",           status: "IN_PROGRESS", progressPercent: 60,  updatedAt: "2026-08-28T14:00:00Z" },
  { topicId: 3, topicTitle: "Business Communication",  status: "IN_PROGRESS", progressPercent: 30,  updatedAt: "2026-08-29T09:00:00Z" },
  { topicId: 4, topicTitle: "Technology & Innovation", status: "NOT_STARTED", progressPercent: 0,   updatedAt: "2026-08-20T00:00:00Z" },
  { topicId: 8, topicTitle: "Daily Conversations",     status: "COMPLETED",   progressPercent: 100, updatedAt: "2026-08-22T16:00:00Z" },
];

const MOCK_PROFILE = {
  id: 1, username: "john_doe", email: "john@linguistai.com",
  avatarUrl: "https://picsum.photos/seed/johndoe/200/200", role: "USER"
};

const MOCK_ADMIN_PROFILE = {
  id: 999, username: "admin", email: "admin@linguistai.com",
  avatarUrl: "https://picsum.photos/seed/admin/200/200", role: "ADMIN"
};

// ─── Exported API Functions ───────────────────────────────────────────────────

export async function login(username, password) {
  await delay(rand());

  // DEBUG: Log the inputs
  console.log('[mockApi] Login attempt:', { username, password });
  console.log('[mockApi] Checking admin:', username === "admin", password === "admin123");

  // Admin account: username "admin" / password "admin123"
  if (username === "admin" && password === "admin123") {
    console.log('[mockApi] ✅ ADMIN LOGIN SUCCESS');
    return {
      accessToken: "mock.jwt.admin.token.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5OTkiLCJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFETUlOIiwiZXhwIjo5OTk5OTk5OTk5fQ.mock",
      refreshToken: "mock.refresh.admin.token",
      user: {
        id: 999,
        username: "admin",
        email: "admin@linguistai.com",
        role: "ADMIN",
        avatarUrl: "https://picsum.photos/seed/admin/200/200"
      }
    };
  }

  // Regular user: accept any other credentials
  console.log('[mockApi] Regular user login');
  return {
    accessToken: "mock.jwt.token.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJqb2huX2RvZSIsInJvbGUiOiJVU0VSIiwiZXhwIjo5OTk5OTk5OTk5fQ.mock",
    refreshToken: "mock.refresh.token",
    user: {
      id: 1,
      username: username || "john_doe",
      email: "john@linguistai.com",
      role: "USER",
      avatarUrl: "https://picsum.photos/seed/johndoe/200/200"
    }
  };
}

export async function register(email, username, password) {
  await delay(rand());
  return { message: "Registration successful! Please login." };
}

export async function getTopics({ keyword = '', level = '', page = 0, size = 20 } = {}) {
  await delay(rand());
  let filtered = [...MOCK_TOPICS];
  if (keyword) filtered = filtered.filter(t =>
    t.title.toLowerCase().includes(keyword.toLowerCase()) ||
    t.description.toLowerCase().includes(keyword.toLowerCase())
  );
  if (level) filtered = filtered.filter(t => t.level === level);
  const total   = filtered.length;
  const content = filtered.slice(page * size, (page + 1) * size);
  return { content, totalPages: Math.ceil(total / size) || 1, totalElements: total, page, size };
}

export async function getTopicDetail(id) {
  await delay(rand());
  const topic = MOCK_TOPICS.find(t => t.id === Number(id));
  if (!topic) throw new Error("Topic not found");
  return topic;
}

export async function getFlashcards(topicId) {
  await delay(rand());
  return MOCK_FLASHCARDS[Number(topicId)] || DEFAULT_FLASHCARDS(Number(topicId));
}

export async function getQuizzesByTopic(topicId) {
  await delay(rand());
  const topic = MOCK_TOPICS.find(t => t.id === Number(topicId));
  return MOCK_QUIZZES[Number(topicId)] || [
    { id: Number(topicId) * 10 + 1, title: `${topic?.title || 'Topic'} Quiz 1` },
    { id: Number(topicId) * 10 + 2, title: `${topic?.title || 'Topic'} Quiz 2` },
  ];
}

export async function getQuizDetail(quizId) {
  await delay(rand());
  return MOCK_QUIZ_DETAILS[Number(quizId)] || GENERIC_QUIZ_DETAIL(quizId);
}

export async function submitQuiz(quizId, answers) {
  await delay(rand());
  const total   = (answers && answers.length) || 5;
  const correct = Math.floor(Math.random() * 3) + 3; // 3–5 correct
  return {
    score: Math.round((correct / total) * 100),
    correctAnswers: correct,
    totalQuestions: total,
    completedAt: new Date().toISOString()
  };
}

export async function getQuizAttempts(quizId, page = 0, size = 20) {
  await delay(rand());
  const content = [
    { id: 1, score: 80,  correctAnswers: 4, totalQuestions: 5, completedAt: "2026-08-29T10:00:00Z" },
    { id: 2, score: 60,  correctAnswers: 3, totalQuestions: 5, completedAt: "2026-08-27T14:00:00Z" },
    { id: 3, score: 100, correctAnswers: 5, totalQuestions: 5, completedAt: "2026-08-25T09:00:00Z" },
  ];
  return { content, totalPages: 1, totalElements: content.length, page, size };
}

export async function getProgress() {
  await delay(rand());
  return MOCK_PROGRESS;
}

export async function getConversations(page = 0, size = 20) {
  await delay(rand());
  return { content: MOCK_CONVERSATIONS, totalPages: 1, totalElements: MOCK_CONVERSATIONS.length, page, size };
}

export async function getConversation(id) {
  await delay(rand());
  return MOCK_CONVERSATIONS.find(c => c.id === Number(id)) || MOCK_CONVERSATIONS[0];
}

export async function getMessages(conversationId) {
  await delay(rand());
  return MOCK_MESSAGES[Number(conversationId)] || [];
}

export async function createConversation() {
  await delay(rand());
  return { id: Date.now(), title: "New conversation", updatedAt: new Date().toISOString() };
}

export async function sendMessage(conversationId, content) {
  await delay(rand());
  const aiReplies = [
    "That's a great point! Let me help you improve your English.",
    "Interesting! Could you tell me more about that?",
    "Well said! Here's some additional vocabulary you might find useful.",
    "Good effort! I noticed one small grammar point — let me explain.",
    "Excellent sentence! Your English is improving rapidly.",
  ];
  return {
    id: Date.now(),
    sender: "AI",
    content: aiReplies[Math.floor(Math.random() * aiReplies.length)],
    correction: null,
    explanation: null,
    createdAt: new Date().toISOString()
  };
}

export async function getProfile() {
  await delay(rand());
  // Check localStorage to determine which profile to return
  const storedUser = localStorage.getItem('linguistai_user');
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser);
      console.log('[mockApi.getProfile] User from localStorage:', user);
      // Return profile matching the logged-in user
      return {
        id: user.id,
        username: user.username,
        email: user.email,
        avatarUrl: user.avatarUrl || (user.role === 'ADMIN' ? MOCK_ADMIN_PROFILE.avatarUrl : MOCK_PROFILE.avatarUrl),
        role: user.role
      };
    } catch (e) {
      console.error('Failed to parse user from localStorage', e);
    }
  }
  return { ...MOCK_PROFILE };
}

export async function uploadAvatar(formData) {
  await delay(rand());
  return { avatarUrl: "https://picsum.photos/seed/newavatar/200/200" };
}

export async function adminGetTopics() {
  await delay(rand());
  return { content: MOCK_TOPICS, totalPages: 1, totalElements: MOCK_TOPICS.length, page: 0, size: 20 };
}

export async function adminCreateTopic(data) {
  await delay(rand());
  return { id: Date.now(), ...data };
}

export async function adminUpdateTopic(id, data) {
  await delay(rand());
  return { id: Number(id), ...data };
}

export async function adminDeleteTopic(id) {
  await delay(rand());
  return { message: "Topic deleted successfully" };
}
