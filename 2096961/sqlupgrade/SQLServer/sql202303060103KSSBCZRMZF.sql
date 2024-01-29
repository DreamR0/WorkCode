USE [ecology]
GO

/****** Object:  Table [dbo].[Custom01_Message]    Script Date: 2023/3/7 18:37:49 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Custom01_Message](
    [id] [int] IDENTITY(1,1) NOT NULL,
    [extNo] [varchar](100) NULL,
    [requestid] [int] NULL,
    [userid] [int] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
