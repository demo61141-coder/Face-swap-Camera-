export interface DemoFaceMeta {
  id: string;
  nameEn: string;
  nameBn: string;
  styleEn: string;
  styleBn: string;
  src: string;
  isUploaded?: boolean;
}

export const DEMO_FACES: DemoFaceMeta[] = [
  {
    id: 'cyber_aria',
    nameEn: 'Cyber Aria',
    nameBn: 'সাইবার আরিয়া',
    styleEn: 'Realistic',
    styleBn: 'বাস্তবসম্মত',
    src: '/avatars/img_char_cyber_aria.jpg',
  },
  {
    id: 'studio_dev',
    nameEn: 'Studio Dev',
    nameBn: 'স্টুডিও দেব',
    styleEn: 'Realistic',
    styleBn: 'বাস্তবসম্মত',
    src: '/avatars/img_char_studio_dev.jpg',
  },
  {
    id: 'neon_pixar',
    nameEn: 'Neon 3D',
    nameBn: 'নিয়ন ৩ডি',
    styleEn: '3D Stylized',
    styleBn: '৩ডি স্টাইল',
    src: '/avatars/img_char_neon_pixar.jpg',
  },
  {
    id: 'royal_knight',
    nameEn: 'Royal Hero',
    nameBn: 'রয়্যাল হিরো',
    styleEn: 'Fantasy',
    styleBn: 'ফ্যান্টাসি',
    src: '/avatars/img_char_royal_knight.jpg',
  },
  {
    id: 'retro_pop',
    nameEn: 'Cel Pop',
    nameBn: 'সেল পপ',
    styleEn: 'Stylized',
    styleBn: 'স্টাইলাইজড',
    src: '/avatars/img_char_retro_pop.jpg',
  },
  {
    id: 'cosmic_sage',
    nameEn: 'Cosmic Sage',
    nameBn: 'কসমিক সেজ',
    styleEn: 'Editorial',
    styleBn: 'এডিটোরিয়াল',
    src: '/avatars/img_char_cosmic_sage.jpg',
  },
];
